package com.kosmanenko.vpo_humanitarian_aid_platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank(message = "Ім'я не може бути порожнім")
    private String fullName;

    @NotBlank(message = "Email не може бути порожнім")
    @Email(message = "Невірний формат email")
    private String email;

    @NotBlank(message = "Пароль не може бути порожнім")
    @Size(min = 6, message = "Пароль має містити мінімум 6 символів")
    private String password;

    private String phone;
    private String city;

    @NotBlank(message = "Оберіть роль")
    private String role;

    private String providerType;
    private String orgName;
}
