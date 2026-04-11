package com.kosmanenko.vpo_humanitarian_aid_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AnnouncementForm {

    @NotBlank(message = "Заголовок є обов'язковим")
    @Size(max = 255, message = "Заголовок не може перевищувати 255 символів")
    private String title;

    @NotBlank(message = "Опис є обов'язковим")
    private String description;

    @NotBlank(message = "Місто є обов'язковим")
    private String city;

    @NotEmpty(message = "Оберіть принаймні одну категорію")
    private List<Long> categoryIds;
    private String donationUrl;
    private Boolean acceptsApplications;
}
