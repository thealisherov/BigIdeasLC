package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductSaleDto {
    private Long id;
    private String productName;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String category;
    private Long branchId;
    private String branchName;
    private Long studentId;
    private String studentName;
    private LocalDateTime createdAt;
}