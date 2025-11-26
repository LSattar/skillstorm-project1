package com.shelfsync.dtos;

import java.math.BigDecimal;

public record ItemDto(Integer id, String sku, String gameTitle, Integer categoryId, Integer companyId,
		BigDecimal weightLbs, BigDecimal cubicFeet) {
}
