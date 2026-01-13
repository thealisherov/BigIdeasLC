package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateProductSaleRequest;
import com.ogabek.istudy.dto.response.ProductSaleDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.ProductCategory;
import com.ogabek.istudy.entity.ProductSale;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.ProductSaleRepository;
import com.ogabek.istudy.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSaleService {
    private final ProductSaleRepository productSaleRepository;
    private final BranchRepository branchRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<ProductSaleDto> getAllSalesByBranch(Long branchId) {
        return productSaleRepository.findByBranchIdWithRelations(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductSaleDto getSaleById(Long id) {
        ProductSale sale = productSaleRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Mahsulot sotilishi topilmadi: " + id));
        return convertToDto(sale);
    }

    @Transactional(readOnly = true)
    public List<ProductSaleDto> getSalesByCategory(Long branchId, String category) {
        ProductCategory productCategory = ProductCategory.valueOf(category.toUpperCase());
        return productSaleRepository.findByBranchIdAndCategory(branchId, productCategory).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductSaleDto> getSalesByStudent(Long studentId) {
        return productSaleRepository.findByStudentId(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductSaleDto> getSalesByDateRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        return productSaleRepository.findByBranchIdAndDateRange(branchId, start, end).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesSummary(Long branchId, Integer year, Integer month) {
        Map<String, Object> summary = new HashMap<>();

        if (year != null && month != null) {
            BigDecimal totalRevenue = productSaleRepository.sumTotalAmountByMonth(branchId, year, month);
            
            summary.put("totalRevenue", totalRevenue);
            summary.put("year", year);
            summary.put("month", month);
        } else {
            BigDecimal totalRevenue = productSaleRepository.sumTotalAmountByBranch(branchId);
            
            summary.put("totalRevenue", totalRevenue);
        }

        summary.put("branchId", branchId);
        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategorySummary(Long branchId) {
        Map<String, Object> summary = new HashMap<>();
        
        for (ProductCategory category : ProductCategory.values()) {
            BigDecimal revenue = productSaleRepository.sumTotalAmountByCategory(branchId, category);
            
            Map<String, BigDecimal> categoryData = new HashMap<>();
            categoryData.put("revenue", revenue);
            
            summary.put(category.name(), categoryData);
        }
        
        return summary;
    }

    @Transactional
    public ProductSaleDto createSale(CreateProductSaleRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        ProductSale sale = new ProductSale();
        sale.setProductName(request.getProductName());
        sale.setDescription(request.getDescription());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setCategory(request.getCategory());
        sale.setBranch(branch);

        // Calculate total amount
        BigDecimal totalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        sale.setTotalAmount(totalAmount);

        // Link to student if provided
        if (request.getStudentId() != null) {
            Student student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + request.getStudentId()));
            sale.setStudent(student);
        }

        ProductSale savedSale = productSaleRepository.save(sale);
        
        return convertToDto(productSaleRepository.findByIdWithRelations(savedSale.getId())
                .orElseThrow(() -> new RuntimeException("Saqlangan mahsulot topilmadi")));
    }

    @Transactional
    public ProductSaleDto updateSale(Long id, CreateProductSaleRequest request) {
        ProductSale sale = productSaleRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Mahsulot sotilishi topilmadi: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        sale.setProductName(request.getProductName());
        sale.setDescription(request.getDescription());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setCategory(request.getCategory());
        sale.setBranch(branch);

        // Recalculate total amount
        BigDecimal totalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        sale.setTotalAmount(totalAmount);

        // Update student link
        if (request.getStudentId() != null) {
            Student student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + request.getStudentId()));
            sale.setStudent(student);
        } else {
            sale.setStudent(null);
        }

        ProductSale updatedSale = productSaleRepository.save(sale);
        
        return convertToDto(productSaleRepository.findByIdWithRelations(updatedSale.getId())
                .orElseThrow(() -> new RuntimeException("Yangilangan mahsulot topilmadi")));
    }

    @Transactional
    public void deleteSale(Long id) {
        if (!productSaleRepository.existsById(id)) {
            throw new RuntimeException("Mahsulot sotilishi topilmadi: " + id);
        }
        productSaleRepository.deleteById(id);
    }

    private ProductSaleDto convertToDto(ProductSale sale) {
        ProductSaleDto dto = new ProductSaleDto();
        dto.setId(sale.getId());
        dto.setProductName(sale.getProductName());
        dto.setDescription(sale.getDescription());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setCategory(sale.getCategory().name());

        if (sale.getBranch() != null) {
            dto.setBranchId(sale.getBranch().getId());
            dto.setBranchName(sale.getBranch().getName());
        }

        if (sale.getStudent() != null) {
            dto.setStudentId(sale.getStudent().getId());
            dto.setStudentName(sale.getStudent().getFirstName() + " " + sale.getStudent().getLastName());
        }

        dto.setCreatedAt(sale.getCreatedAt());
        return dto;
    }
}