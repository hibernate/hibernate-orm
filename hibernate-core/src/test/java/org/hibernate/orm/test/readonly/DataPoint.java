/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;

/**
 * @author Gavin King
 */
@Entity
@DynamicUpdate
public class DataPoint implements Serializable {
	@Id
	@GeneratedValue
	private long id;
	@Column(name = "xval", nullable = false, precision = 25, scale = 19)
	private BigDecimal x;
	@Column(name = "yval", nullable = false, precision = 25, scale = 19)
	private BigDecimal y;
	private String description;

	public DataPoint() {}

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
