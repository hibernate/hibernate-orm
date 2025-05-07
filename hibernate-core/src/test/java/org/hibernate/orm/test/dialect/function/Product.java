/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Strong Liu
 */
@Entity
public class Product {
	@Id
	private Long id;
	private int length;
	private long weight;
	private BigDecimal price;
	private Date date;

	public Product() {
	}

	public Product(Long id) {
		this.id = id;
	}

	public Product(Long id, int length, long weight, BigDecimal price, Date date) {
		this.id = id;
		this.length = length;
		this.weight = weight;
		this.price = price;
		this.date = date;
	}

	public Long getId() {
		return id;
	}

	public void setId( Long id ) {
		this.id = id;
	}

	public int getLength() {
		return length;
	}

	public void setLength( int length ) {
		this.length = length;
	}

	public long getWeight() {
		return weight;
	}

	public void setWeight( long weight ) {
		this.weight = weight;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice( BigDecimal price ) {
		this.price = price;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
