package com.kosmanenko.vpo_humanitarian_aid_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProfileForm {

    @NotBlank(message = "Це поле є обов'язковим")
    @Size(max = 255, message = "ПІБ не може перевищувати 255 символів")
    @Pattern.List({
        @Pattern(regexp = "^[\\p{L} ]+$", message = "ПІБ може містити лише літери та пробіли"),
        @Pattern(regexp = "^\\S+(\\s+\\S+)+$", message = "Введіть повне ім'я (щонайменше два слова)")
    })
    private String fullName;

    @NotBlank(message = "Це поле є обов'язковим")
    @Pattern(regexp = "^\\+[0-9]{10,14}$", message = "Введіть коректний номер телефону (наприклад: +380501234567)")
    private String phone;

    @NotBlank(message = "Це поле є обов'язковим")
    private String city;

    private String orgName;
    private String orgDescription;
    private Boolean isProfilePublic;
    private List<Long> providerCategoryIds;
}
