// src/main/java/com/ogabek/istudy/service/StudentService.java
package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateStudentRequest;
import com.ogabek.istudy.dto.response.GroupDto;
import com.ogabek.istudy.dto.response.PaymentDto;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.dto.response.UnpaidStudentDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Group;
import com.ogabek.istudy.entity.Payment;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.GroupRepository;
import com.ogabek.istudy.repository.PaymentRepository;
import com.ogabek.istudy.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;
    private final BranchRepository branchRepository;
    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchIdWithBranch(branchId).stream()
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByBranch(Long branchId, Integer year, Integer month) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        return studentRepository.findByBranchIdWithBranch(branchId).stream()
                .map(student -> convertToDto(student, targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByGroup(Long groupId, Integer year, Integer month) {
        Group group = groupRepository.findByIdWithAllRelations(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        if (group.getStudents() == null || group.getStudents().isEmpty()) {
            return new ArrayList<>();
        }

        return group.getStudents().stream()
                .map(student -> convertToDto(student, targetYear, targetMonth))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UnpaidStudentDto> getUnpaidStudents(Long branchId, Integer year, Integer month) {
        List<UnpaidStudentDto> result = new ArrayList<>();
        List<Group> branchGroups = groupRepository.findByBranchIdWithAllRelations(branchId);

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();

        for (Group group : branchGroups) {
            if (group.getStudents() != null) {
                for (Student student : group.getStudents()) {
                    BigDecimal totalPaid;

                    if (year == null || month == null) {
                        totalPaid = paymentRepository.findByStudentIdWithRelations(student.getId())
                                .stream()
                                .filter(payment -> payment.getGroup().getId().equals(group.getId()))
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    } else {
                        totalPaid = paymentRepository.getTotalPaidByStudentInGroupForMonth(
                                student.getId(), group.getId(), targetYear, targetMonth);
                        totalPaid = totalPaid != null ? totalPaid : BigDecimal.ZERO;
                    }

                    BigDecimal remainingAmount = group.getPrice().subtract(totalPaid);

                    // NEW: Check if payment is actually overdue based on payment day
                    boolean isOverdue = isPaymentOverdue(student, targetYear, targetMonth);

                    if (remainingAmount.compareTo(BigDecimal.ZERO) > 0 && isOverdue) {
                        result.add(new UnpaidStudentDto(
                                student.getId(),
                                student.getFirstName(),
                                student.getLastName(),
                                student.getPhoneNumber(),
                                student.getParentPhoneNumber(),
                                remainingAmount,
                                group.getId(),
                                group.getName()));
                    }
                }
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> searchStudentsByName(Long branchId, String name) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchIdAndFullName(branchId, name).stream()
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getStudentPaymentHistory(Long studentId) {
        return paymentRepository.findByStudentIdWithRelations(studentId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getStudentGroups(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        return groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .map(this::convertGroupToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentStatistics(Long branchId) {
        List<Student> allStudents = studentRepository.findByBranchId(branchId);
        LocalDate paymentPeriod = getCurrentPaymentPeriod();

        // Count students by payment status
        long paidCount = 0;
        long unpaidCount = 0;
        long upcomingCount = 0;
        long overdueCount = 0;

        for (Student student : allStudents) {
            StudentDto dto = convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
            String status = dto.getPaymentStatus();

            switch (status) {
                case "PAID":
                    paidCount++;
                    break;
                case "OVERDUE":
                    overdueCount++;
                    break;
                case "UPCOMING":
                    upcomingCount++;
                    break;
                default:
                    unpaidCount++;
                    break;
            }
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalStudents", allStudents.size());
        statistics.put("paidStudents", paidCount);
        statistics.put("unpaidStudents", unpaidCount);
        statistics.put("upcomingStudents", upcomingCount);
        statistics.put("overdueStudents", overdueCount);
        statistics.put("paymentRate",
                allStudents.size() > 0
                        ? (double) paidCount / allStudents.size() * 100
                        : 0);

        return statistics;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getRecentStudents(Long branchId, int limit) {
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return studentRepository.findByBranchId(branchId).stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(limit)
                .map(student -> convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return convertToDto(student, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id, Integer year, Integer month) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        int targetYear = year != null ? year : paymentPeriod.getYear();
        int targetMonth = month != null ? month : paymentPeriod.getMonthValue();
        return convertToDto(student, targetYear, targetMonth);
    }

    @Transactional
    public StudentDto createStudent(CreateStudentRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        Student student = new Student();
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);
        student.setPaymentDayOfMonth(request.getPaymentDayOfMonth()); // NEW

        Student savedStudent = studentRepository.save(student);

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            for (Long groupId : request.getGroupIds()) {
                Group group = groupRepository.findByIdWithAllRelations(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

                if (!group.getBranch().getId().equals(request.getBranchId())) {
                    throw new RuntimeException(
                            "Group " + groupId + " does not belong to branch " + request.getBranchId());
                }

                if (group.getStudents() == null) {
                    group.setStudents(new HashSet<>());
                }
                group.getStudents().add(savedStudent);
                groupRepository.save(group);
            }
        }

        LocalDate paymentPeriod = getCurrentPaymentPeriod();
        return convertToDto(savedStudent, paymentPeriod.getYear(), paymentPeriod.getMonthValue());
    }

    @Transactional
    public StudentDto updateStudent(Long id, CreateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setParentPhoneNumber(request.getParentPhoneNumber());
        student.setBranch(branch);
        student.setPaymentDayOfMonth(request.getPaymentDayOfMonth()); // NEW

        Student savedStudent = studentRepository.save(student);

        List<Group> currentGroups = groupRepository.findByBranchIdWithAllRelations(branch.getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .collect(Collectors.toList());

        for (Group group : currentGroups) {
            group.getStudents().remove(student);
            groupRepository.save(group);
        }

        if (request.getGroupIds() != null && !request.getGroupIds().isEmpty()) {
            for (Long groupId : request.getGroupIds()) {
                Group group = groupRepository.findByIdWithAllRelations(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

                if (!group.getBranch().getId().equals(request.getBranchId())) {
                    throw new RuntimeException(
                            "Group " + groupId + " does not belong to branch " + request.getBranchId());
                }

                if (group.getStudents() == null) {
                    group.setStudents(new HashSet<>());
                }
                group.getStudents().add(savedStudent);
                groupRepository.save(group);
            }
        }

        LocalDate now = LocalDate.now();
        return convertToDto(savedStudent, now.getYear(), now.getMonthValue());
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("O'quvchi topilmadi: " + id));

        // Remove from all groups first
        List<Group> studentGroups = groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId()).stream()
                .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                .collect(Collectors.toList());

        for (Group group : studentGroups) {
            group.getStudents().remove(student);
            groupRepository.save(group);
        }

        // Soft delete by setting deleted flag
        student.setDeleted(true);
        studentRepository.save(student);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private LocalDate getCurrentPaymentPeriod() {
        LocalDate now = LocalDate.now();
        if (now.getDayOfMonth() < 5) {
            return now.minusMonths(1);
        }
        return now;
    }

    /**
     * NEW: Calculate the next payment due date for a student
     */
    private LocalDate calculateNextDueDate(Student student, int year, int month) {
        if (student.getPaymentDayOfMonth() == null) {
            return null; // No payment day set
        }

        LocalDate today = LocalDate.now();
        int dayOfMonth = student.getPaymentDayOfMonth();

        try {
            LocalDate dueDate = LocalDate.of(year, month, dayOfMonth);

            // If the calculated due date has already passed, calculate for next month
            if (dueDate.isBefore(today)) {
                LocalDate nextMonth = dueDate.plusMonths(1);
                try {
                    return LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), dayOfMonth);
                } catch (Exception e) {
                    // If day doesn't exist in next month, use last day
                    return nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
                }
            }

            return dueDate;
        } catch (Exception e) {
            // Handle invalid dates (e.g., Feb 30)
            LocalDate firstOfMonth = LocalDate.of(year, month, 1);
            return firstOfMonth.withDayOfMonth(firstOfMonth.lengthOfMonth());
        }
    }

    /**
     * NEW: Check if a payment is overdue for a student in a specific month
     */
    private boolean isPaymentOverdue(Student student, int year, int month) {
        if (student.getPaymentDayOfMonth() == null) {
            return false; // Can't be overdue if no payment day is set
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate;

        try {
            dueDate = LocalDate.of(year, month, student.getPaymentDayOfMonth());
        } catch (Exception e) {
            // If day doesn't exist in month, use last day of month
            dueDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        }

        return today.isAfter(dueDate);
    }

    private StudentDto convertToDto(Student student, int year, int month) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPhoneNumber(student.getPhoneNumber());
        dto.setParentPhoneNumber(student.getParentPhoneNumber());
        dto.setPaymentDayOfMonth(student.getPaymentDayOfMonth()); // NEW

        if (student.getBranch() != null) {
            dto.setBranchId(student.getBranch().getId());
            dto.setBranchName(student.getBranch().getName());
        }

        dto.setCreatedAt(student.getCreatedAt());

        // NEW: Calculate next due date
        LocalDate nextDueDate = calculateNextDueDate(student, year, month);
        dto.setNextDueDate(nextDueDate);

        if (student.getBranch() != null) {
            List<Group> studentGroups = groupRepository.findByBranchIdWithAllRelations(student.getBranch().getId())
                    .stream()
                    .filter(group -> group.getStudents() != null && group.getStudents().contains(student))
                    .collect(Collectors.toList());

            List<StudentDto.GroupInfo> groupInfos = studentGroups.stream()
                    .map(group -> {
                        String teacherName = group.getTeacher() != null
                                ? group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName()
                                : null;
                        return new StudentDto.GroupInfo(
                                group.getId(),
                                group.getName(),
                                group.getPrice(),
                                teacherName);
                    })
                    .collect(Collectors.toList());

            dto.setGroups(groupInfos);
        }

        calculatePaymentStatus(dto, student.getId(), year, month);

        return dto;
    }

    /**
     * UPDATED: Calculate payment status with due date awareness
     */
    private void calculatePaymentStatus(StudentDto dto, Long studentId, int year, int month) {
        Boolean hasPaid = studentRepository.hasStudentPaidInMonth(studentId, year, month);
        dto.setHasPaidInMonth(hasPaid != null ? hasPaid : false);

        BigDecimal totalPaid = studentRepository.getTotalPaidByStudentInMonth(studentId, year, month);
        dto.setTotalPaidInMonth(totalPaid != null ? totalPaid : BigDecimal.ZERO);

        BigDecimal expectedPayment = studentRepository.getExpectedMonthlyPaymentForStudent(studentId);
        expectedPayment = expectedPayment != null ? expectedPayment : BigDecimal.ZERO;

        BigDecimal remaining = expectedPayment.subtract(dto.getTotalPaidInMonth());
        dto.setRemainingAmount(remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO);

        // NEW: Enhanced status calculation with due date awareness
        LocalDate nextDueDate = dto.getNextDueDate();
        LocalDate today = LocalDate.now();

        if (dto.getTotalPaidInMonth().compareTo(expectedPayment) >= 0) {
            // Fully paid
            dto.setPaymentStatus("PAID");
        } else if (nextDueDate == null) {
            // No due date set - use old logic
            if (dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) == 0) {
                dto.setPaymentStatus("UNPAID");
            } else {
                dto.setPaymentStatus("PARTIAL");
            }
        } else {
            // Has due date - check if overdue
            boolean hasPartialPayment = dto.getTotalPaidInMonth().compareTo(BigDecimal.ZERO) > 0;

            if (today.isBefore(nextDueDate)) {
                // Payment not yet due
                dto.setPaymentStatus(hasPartialPayment ? "PARTIAL" : "UPCOMING");
            } else {
                // Payment is past due date
                long daysOverdue = ChronoUnit.DAYS.between(nextDueDate, today);

                if (daysOverdue >= 7) {
                    // More than a week overdue
                    dto.setPaymentStatus("OVERDUE");
                } else {
                    // Less than a week overdue
                    dto.setPaymentStatus(hasPartialPayment ? "PARTIAL" : "UNPAID");
                }
            }
        }

        LocalDateTime lastPaymentDate = studentRepository.getLastPaymentDate(studentId);
        dto.setLastPaymentDate(lastPaymentDate);
    }

    private PaymentDto convertPaymentToDto(Payment payment) {
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
        dto.setStatus(payment.getStatus().name());
        if (payment.getBranch() != null) {
            dto.setBranchId(payment.getBranch().getId());
            dto.setBranchName(payment.getBranch().getName());
        }
        dto.setPaymentYear(payment.getPaymentYear());
        dto.setPaymentMonth(payment.getPaymentMonth());
        dto.setDueDate(payment.getDueDate()); // NEW
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    private GroupDto convertGroupToDto(Group group) {
        GroupDto dto = new GroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setPrice(group.getPrice());

        if (group.getTeacher() != null) {
            dto.setTeacherId(group.getTeacher().getId());
            dto.setTeacherName(group.getTeacher().getFirstName() + " " + group.getTeacher().getLastName());
        }

        if (group.getBranch() != null) {
            dto.setBranchId(group.getBranch().getId());
            dto.setBranchName(group.getBranch().getName());
        }

        dto.setStartTime(group.getStartTime());
        dto.setEndTime(group.getEndTime());

        if (group.getDaysOfWeek() != null && !group.getDaysOfWeek().isEmpty()) {
            dto.setDaysOfWeek(Arrays.asList(group.getDaysOfWeek().split(",")));
        } else {
            dto.setDaysOfWeek(new ArrayList<>());
        }

        dto.setStudentCount(group.getStudents() != null ? group.getStudents().size() : 0);
        dto.setCreatedAt(group.getCreatedAt());
        return dto;
    }
}