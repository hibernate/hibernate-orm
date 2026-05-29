/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"xval", "yval"}))
public class DataPoint implements Serializable {
	@Id
	@GeneratedValue
	private long id;

	@Column(name = "xval", nullable = false, length = 4)
	private BigDecimal x;

	@Column(name = "yval", nullable = false, length = 4)
	private BigDecimal y;

	private String description;

	public DataPoint() {
	}

	public DataPoint(BigDecimal x, BigDecimal y, String description) {
		this.x = x;
		this.y = y;
		this.description = description;
	}

	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the x.
	 */
	public BigDecimal getX() {
		return x;
	}
	/**
	 * @param x The x to set.
	 */
	public void setX(BigDecimal x) {
		this.x = x;
	}
	/**
	 * @return Returns the y.
	 */
	public BigDecimal getY() {
		return y;
	}
	/**
	 * @param y The y to set.
	 */
	public void setY(BigDecimal y) {
		this.y = y;
	}

	void exception() throws Exception {
		throw new Exception("foo");
	}
}
