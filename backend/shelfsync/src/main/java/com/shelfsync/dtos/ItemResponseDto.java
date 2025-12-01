package com.shelfsync.dtos;

import java.math.BigDecimal;
import com.shelfsync.models.Category;
import com.shelfsync.models.Company;

public record ItemResponseDto(Integer id, String sku, String gameTitle, Category category, Company company,
		BigDecimal weightLbs, BigDecimal cubicFeet) {

}
