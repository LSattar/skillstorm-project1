package com.shelfsync.dtos;

import jakarta.validation.constraints.NotBlank;

public record CategoryDto(Integer categoryId, @NotBlank(message= "category name is required")String categoryName) {

}
