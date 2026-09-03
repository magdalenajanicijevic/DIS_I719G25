package com.ecommerce.productservice.dto;

import java.math.BigDecimal;

import com.ecommerce.productservice.entity.Category;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateProductRequest {

	@NotBlank(message = "Product name is required.")
	private String name;

	private String description;

	@NotNull(message = "Price is required.")
	@DecimalMin(value = "0.01", message = "Price must be greater than zero.")
	private BigDecimal price;

	@NotNull(message = "Category is required.")
	private Category category;

	@NotNull(message = "Product status is required.")
	private Boolean active;

}
