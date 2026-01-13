package com.ogabek.istudy.dto.request;

import com.ogabek.istudy.entity.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductSaleRequest {
    @NotBlank(message = "Mahsulot nomi majburiy")
    @Size(min = 2, max = 100, message = "Mahsulot nomi 2-100 harfdan iborat bo'lishi kerak")
    private String productName;

    @Size(max = 500, message = "Tavsif 500 harfdan oshmasligi kerak")
    private String description;

    @NotNull(message = "Miqdor majburiy")
    @Min(value = 1, message = "Miqdor 1 dan kichik bo'lmasligi kerak")
    private Integer quantity;

    @NotNull(message = "Birlik narxi majburiy")
    @DecimalMin(value = "0.0", inclusive = false, message = "Birlik narxi 0 dan katta bo'lishi kerak")
    private BigDecimal unitPrice;

    @NotNull(message = "Kategoriya majburiy")
    private ProductCategory category;

    @NotNull(message = "Filial majburiy")
    private Long branchId;

    private Long studentId; // Optional - if sold to a specific student
}