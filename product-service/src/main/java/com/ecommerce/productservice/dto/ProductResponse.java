package com.ecommerce.productservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ecommerce.productservice.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
	
	private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Category category;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
