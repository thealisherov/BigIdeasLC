package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateProductSaleRequest;
import com.ogabek.istudy.dto.response.ProductSaleDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.ProductSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductSaleController {

    private final ProductSaleService productSaleService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<ProductSaleDto>> getAllSalesByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ProductSaleDto> sales = productSaleService.getAllSalesByBranch(branchId);
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSaleDto> getSaleById(@PathVariable Long id) {
        ProductSaleDto sale = productSaleService.getSaleById(id);
        if (!branchAccessControl.hasAccessToBranch(sale.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(sale);
    }

    @GetMapping("/by-category")
    public ResponseEntity<List<ProductSaleDto>> getSalesByCategory(
            @RequestParam Long branchId,
            @RequestParam String category) {
        
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ProductSaleDto> sales = productSaleService.getSalesByCategory(branchId, category);
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/by-student/{studentId}")
    public ResponseEntity<List<ProductSaleDto>> getSalesByStudent(@PathVariable Long studentId) {
        List<ProductSaleDto> sales = productSaleService.getSalesByStudent(studentId);
        
        if (!sales.isEmpty() && !branchAccessControl.hasAccessToBranch(sales.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(sales);
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<ProductSaleDto>> getSalesByDateRange(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ProductSaleDto> sales = productSaleService.getSalesByDateRange(branchId, startDate, endDate);
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSalesSummary(
            @RequestParam Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> summary = productSaleService.getSalesSummary(branchId, year, month);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/category-summary")
    public ResponseEntity<Map<String, Object>> getCategorySummary(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> summary = productSaleService.getCategorySummary(branchId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<ProductSaleDto> createSale(@Valid @RequestBody CreateProductSaleRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductSaleDto sale = productSaleService.createSale(request);
        return ResponseEntity.ok(sale);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSaleDto> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductSaleRequest request) {

        ProductSaleDto existingSale = productSaleService.getSaleById(id);
        if (!branchAccessControl.hasAccessToBranch(existingSale.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProductSaleDto sale = productSaleService.updateSale(id, request);
        return ResponseEntity.ok(sale);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        ProductSaleDto sale = productSaleService.getSaleById(id);
        if (!branchAccessControl.hasAccessToBranch(sale.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        productSaleService.deleteSale(id);
        return ResponseEntity.ok().build();
    }
}