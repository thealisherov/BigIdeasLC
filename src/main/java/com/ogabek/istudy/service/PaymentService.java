package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreatePaymentRequest;
import com.ogabek.istudy.dto.response.PaymentDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByBranch(Long branchId) {
        return paymentRepository.findByBranchIdWithAllRelations(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // NEW: Get payments by category
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByBranchAndCategory(Long branchId, String category) {
        PaymentCategory paymentCategory = PaymentCategory.valueOf(category.toUpperCase());
        return paymentRepository.findByBranchIdAndCategoryWithAllRelations(branchId, paymentCategory).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // NEW: Get payments by category and month
    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByBranchAndCategoryAndMonth(Long branchId, String category, int year, int month) {
        PaymentCategory paymentCategory = PaymentCategory.valueOf(category.toUpperCase());
        return paymentRepository.findByBranchIdAndCategoryAndMonthWithAllRelations(branchId, paymentCategory, year, month).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByStudent(Long studentId) {
        return paymentRepository.findByStudentIdWithRelations(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return convertToDto(payment);
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + request.getStudentId()));

        Group group = groupRepository.findByIdWithAllRelations(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + request.getGroupId()));

        if (group.getStudents() == null || !group.getStudents().contains(student)) {
            throw new RuntimeException("O'quvchi bu guruhda yo'q!");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("To'lov miqdori 0 dan katta bo'lishi kerak!");
        }

        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setGroup(group);
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setCategory(PaymentCategory.valueOf(request.getCategory().toUpperCase()));
        payment.setBranch(branch);
        payment.setPaymentYear(request.getPaymentYear());
        payment.setPaymentMonth(request.getPaymentMonth());

        // SIMPLE: Calculate due date
        LocalDate dueDate = calculateDueDate(student, request.getPaymentYear(), request.getPaymentMonth());
        payment.setDueDate(dueDate);

        Payment savedPayment = paymentRepository.save(payment);

        Payment paymentWithRelations = paymentRepository.findByIdWithAllRelations(savedPayment.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch created payment"));

        return convertToDto(paymentWithRelations);
    }

    // SIMPLE: Calculate when payment is due
    private LocalDate calculateDueDate(Student student, int year, int month) {
        // Use student's payment day if available
        int dayOfMonth = student.getPaymentDayOfMonth() != null
                ? student.getPaymentDayOfMonth()
                : 1; // Default to 1st if not set

        try {
            return LocalDate.of(year, month, dayOfMonth);
        } catch (Exception e) {
            // Handle invalid dates (e.g., Feb 30)
            return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        }
    }


    @Transactional
    public PaymentDto updatePaymentAmount(Long id, BigDecimal newAmount) {
        Payment payment = paymentRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));

        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("To'lov miqdori 0 dan katta bo'lishi kerak!");
        }

        payment.setAmount(newAmount);
        Payment savedPayment = paymentRepository.save(payment);

        Payment updatedPaymentWithRelations = paymentRepository.findByIdWithAllRelations(savedPayment.getId())
                .orElseThrow(() -> new RuntimeException("Failed to fetch updated payment"));

        return convertToDto(updatedPaymentWithRelations);
    }

    @Transactional
    public void deletePayment(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByDateRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        return paymentRepository.findByBranchIdAndCreatedAtBetweenWithRelations(
                        branchId,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByMonth(Long branchId, int year, int month) {
        return paymentRepository.findByBranchIdAndPaymentYearAndPaymentMonthWithRelations(branchId, year, month)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> searchPaymentsByStudentName(Long branchId, String studentName) {
        return paymentRepository.findByBranchIdWithAllRelations(branchId).stream()
                .filter(payment -> {
                    if (payment.getStudent() != null) {
                        String fullName = payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName();
                        return fullName.toLowerCase().contains(studentName.toLowerCase());
                    }
                    return false;
                })
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getRecentPayments(Long branchId, int limit) {
        return paymentRepository.findByBranchIdWithAllRelations(branchId).stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private PaymentDto convertToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());

        if (payment.getStudent() != null) {
            dto.setStudentId(payment.getStudent().getId());
            dto.setStudentName(payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName());
        }

        if (payment.getGroup() != null) {
            dto.setGroupId(payment.getGroup().getId());
            dto.setGroupName(payment.getGroup().getName());
        }

        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setCategory(payment.getCategory().name());
        dto.setStatus(payment.getStatus().name());

        if (payment.getBranch() != null) {
            dto.setBranchId(payment.getBranch().getId());
            dto.setBranchName(payment.getBranch().getName());
        }

        dto.setPaymentYear(payment.getPaymentYear());
        dto.setPaymentMonth(payment.getPaymentMonth());
        dto.setDueDate(payment.getDueDate());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}