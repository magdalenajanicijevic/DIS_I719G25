package com.ecommerce.productservice.dto;

import java.math.BigDecimal;

import com.ecommerce.productservice.entity.Category;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

	@NotBlank(message = "Product name is required.")
	private String name;

	private String description;

	@NotNull(message = "Price is required.")
	@DecimalMin(value = "0.01", message = "Price must be greater than zero.")
	private BigDecimal price;

	@NotNull(message = "Category is required.")
	private Category category;

}
