package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeacherRequest {
    @NotBlank(message = "Ism kiritish majburiy")
    @Size(min = 2, max = 50, message = "Ism 2-50 harfdan iborat bo'lishi shart")
    private String firstName;

    @NotBlank(message = "Familiya kiritish majburiy")
    @Size(min = 2, max = 50, message = "Familiya 2-50 harfdan iborat bo'lishi shart")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String phoneNumber;

    @NotNull(message = "Filial kiritish majburiy")
    private Long branchId;
}