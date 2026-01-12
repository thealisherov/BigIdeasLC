package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateSalaryPaymentRequest;
import com.ogabek.istudy.dto.response.GroupSalaryInfo;
import com.ogabek.istudy.dto.response.SalaryCalculationDto;
import com.ogabek.istudy.dto.response.TeacherSalaryHistoryDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPaymentDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherSalaryService {
    private final TeacherSalaryPaymentRepository salaryPaymentRepository;
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public SalaryCalculationDto calculateTeacherSalary(Long teacherId, int year, int month) {
        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        List<Group> teacherGroups = groupRepository.findByTeacherIdWithRelations(teacherId);

        List<GroupSalaryInfo> groupInfos = new ArrayList<>();
        BigDecimal totalSalary = BigDecimal.ZERO;
        int totalPaidStudents = 0;

        for (Group group : teacherGroups) {
            int totalStudentsInGroup = group.getStudents() != null ? group.getStudents().size() : 0;
            int paidStudentCount = 0;
            BigDecimal groupSalaryAmount = BigDecimal.ZERO;
            BigDecimal teacherSalaryPerStudent = group.getTeacherSalaryPerStudent() != null ?
                    group.getTeacherSalaryPerStudent() : BigDecimal.ZERO;

            if (group.getStudents() != null) {
                for (Student student : group.getStudents()) {
                    BigDecimal studentGroupPayment = paymentRepository.getTotalPaidByStudentInGroupForMonth(
                            student.getId(), group.getId(), year, month);

                    // Only count students who have paid
                    if (studentGroupPayment != null && studentGroupPayment.compareTo(BigDecimal.ZERO) > 0) {
                        paidStudentCount++;
                        // Teacher gets fixed amount per paid student
                        groupSalaryAmount = groupSalaryAmount.add(teacherSalaryPerStudent);
                    }
                }
            }

            totalPaidStudents += paidStudentCount;
            totalSalary = totalSalary.add(groupSalaryAmount);

            BigDecimal groupPrice = group.getPrice() != null ? group.getPrice() : BigDecimal.ZERO;

            GroupSalaryInfo groupInfo = new GroupSalaryInfo(
                    group.getId(),
                    group.getName(),
                    paidStudentCount,
                    groupSalaryAmount,
                    totalStudentsInGroup,
                    groupPrice
            );
            groupInfos.add(groupInfo);
        }

        BigDecimal alreadyPaid = salaryPaymentRepository.sumByTeacherAndYearAndMonth(teacherId, year, month);
        alreadyPaid = alreadyPaid != null ? alreadyPaid : BigDecimal.ZERO;

        BigDecimal remainingAmount = totalSalary.subtract(alreadyPaid);
        remainingAmount = remainingAmount.compareTo(BigDecimal.ZERO) > 0 ? remainingAmount : BigDecimal.ZERO;

        SalaryCalculationDto dto = new SalaryCalculationDto();
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
        dto.setYear(year);
        dto.setMonth(month);
        dto.setBaseSalary(BigDecimal.ZERO); // No longer used
        dto.setPaymentBasedSalary(totalSalary);
        dto.setTotalSalary(totalSalary);
        dto.setTotalStudentPayments(BigDecimal.ZERO); // Not relevant in new system
        dto.setTotalStudents(totalPaidStudents);
        dto.setAlreadyPaid(alreadyPaid);
        dto.setRemainingAmount(remainingAmount);
        dto.setBranchId(teacher.getBranch().getId());
        dto.setBranchName(teacher.getBranch().getName());
        dto.setGroups(groupInfos);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<SalaryCalculationDto> calculateSalariesForBranch(Long branchId, int year, int month) {
        List<Teacher> teachers = teacherRepository.findByBranchIdWithBranch(branchId);

        return teachers.stream()
                .map(teacher -> calculateTeacherSalary(teacher.getId(), year, month))
                .collect(Collectors.toList());
    }

    @Transactional
    public TeacherSalaryPaymentDto createSalaryPayment(CreateSalaryPaymentRequest request) {
        Teacher teacher = teacherRepository.findByIdWithBranch(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + request.getTeacherId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("To'lov miqdori 0 dan katta bo'lishi kerak!");
        }

        TeacherSalaryPayment payment = new TeacherSalaryPayment();
        payment.setTeacher(teacher);
        payment.setYear(request.getYear());
        payment.setMonth(request.getMonth());
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setBranch(branch);

        TeacherSalaryPayment savedPayment = salaryPaymentRepository.save(payment);
        return convertPaymentToDto(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getSalaryPaymentsByBranch(Long branchId) {
        return salaryPaymentRepository.findByBranchIdWithDetails(branchId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getSalaryPaymentsByTeacher(Long teacherId) {
        return salaryPaymentRepository.findByTeacherIdWithDetails(teacherId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getPaymentsForTeacherAndMonth(Long teacherId, int year, int month) {
        return salaryPaymentRepository.findByTeacherAndYearAndMonthWithDetails(teacherId, year, month).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryHistoryDto> getTeacherSalaryHistory(Long teacherId) {
        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        List<Object[]> yearMonthPairs = salaryPaymentRepository.findDistinctYearMonthByTeacherId(teacherId);
        List<TeacherSalaryHistoryDto> history = new ArrayList<>();

        for (Object[] pair : yearMonthPairs) {
            int year = (Integer) pair[0];
            int month = (Integer) pair[1];

            SalaryCalculationDto calculation = calculateTeacherSalary(teacherId, year, month);

            BigDecimal totalPaid = salaryPaymentRepository.sumByTeacherAndYearAndMonth(teacherId, year, month);
            LocalDateTime lastPaymentDate = salaryPaymentRepository.getLastPaymentDate(teacherId, year, month);
            int paymentCount = salaryPaymentRepository.countPaymentsByTeacherAndYearAndMonth(teacherId, year, month);

            totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
            BigDecimal remainingAmount = calculation.getTotalSalary().subtract(totalPaid);
            remainingAmount = remainingAmount.compareTo(BigDecimal.ZERO) > 0 ? remainingAmount : BigDecimal.ZERO;

            TeacherSalaryHistoryDto historyItem = new TeacherSalaryHistoryDto(
                    teacherId,
                    teacher.getFirstName() + " " + teacher.getLastName(),
                    year,
                    month,
                    calculation.getTotalSalary(),
                    totalPaid,
                    remainingAmount,
                    remainingAmount.compareTo(BigDecimal.ZERO) == 0,
                    lastPaymentDate,
                    paymentCount
            );

            history.add(historyItem);
        }

        history.sort((a, b) -> {
            int yearCompare = Integer.compare(b.getYear(), a.getYear());
            if (yearCompare != 0) return yearCompare;
            return Integer.compare(b.getMonth(), a.getMonth());
        });

        return history;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRemainingAmountForTeacher(Long teacherId, int year, int month) {
        SalaryCalculationDto calculation = calculateTeacherSalary(teacherId, year, month);
        return calculation.getRemainingAmount();
    }

    @Transactional
    public void deleteSalaryPayment(Long paymentId) {
        if (!salaryPaymentRepository.existsById(paymentId)) {
            throw new RuntimeException("Salary payment not found with id: " + paymentId);
        }
        salaryPaymentRepository.deleteById(paymentId);
    }

    private TeacherSalaryPaymentDto convertPaymentToDto(TeacherSalaryPayment payment) {
        TeacherSalaryPaymentDto dto = new TeacherSalaryPaymentDto();
        dto.setId(payment.getId());
        dto.setTeacherId(payment.getTeacher().getId());
        dto.setTeacherName(payment.getTeacher().getFirstName() + " " + payment.getTeacher().getLastName());
        dto.setYear(payment.getYear());
        dto.setMonth(payment.getMonth());
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setBranchId(payment.getBranch().getId());
        dto.setBranchName(payment.getBranch().getName());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}