package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateStudentRequest {
    @NotBlank(message = "Ism kiritish majburiy")
    @Size(min = 2, max = 50, message = "Ism 2-50 harfdan iborat bo'lishi shart")
    private String firstName;

    @NotBlank(message = "Familiya kiritish majburiy")
    @Size(min = 2, max = 50, message = "Familiya 2-50 harfdan iborat bo'lishi shart")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String phoneNumber;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String parentPhoneNumber;

    @NotNull(message = "Filial kiritish majburiy")
    private Long branchId;

    private List<Long> groupIds = new ArrayList<>();

    // NEW: When does this student pay each month? (1-31)
    @Min(value = 1, message = "To'lov kuni 1 dan kichik bo'lmasligi kerak")
    @Max(value = 31, message = "To'lov kuni 31 dan katta bo'lmasligi kerak")
    private Integer paymentDayOfMonth;
}