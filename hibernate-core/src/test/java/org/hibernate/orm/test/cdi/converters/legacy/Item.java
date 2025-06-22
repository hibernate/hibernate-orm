/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import java.math.BigDecimal;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Etienne Miret
 */
@Entity
public class Item {

	@Id
	@GeneratedValue
	private Long id;

	@Convert( converter = BigDecimalToStringConverter.class )
	private BigDecimal price;

	private String description;

	@Convert( converter = AddTenConverter.class )
	private Integer quantity;

	public Long getId() {
		return id;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(final BigDecimal price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(final Integer quantity) {
		this.quantity = quantity;
	}

	/*
	 * For those who hate auto (un)boxing.
	 */
	public void setQuantity(final int quantity) {
		this.quantity = Integer.valueOf( quantity );
	}

}
