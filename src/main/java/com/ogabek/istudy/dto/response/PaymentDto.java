// src/main/java/com/ogabek/istudy/dto/response/PaymentDto.java
package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long groupId;
    private String groupName;
    private BigDecimal amount;
    private String description;
    private String category;
    private String status;
    private Long branchId;
    private String branchName;
    private Integer paymentYear;
    private Integer paymentMonth;
    private LocalDate dueDate;  // NEW FIELD
    private LocalDateTime createdAt;
}