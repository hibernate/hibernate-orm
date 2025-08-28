/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.rowid;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Gavin King
 */
public class Point implements Serializable {
	private BigDecimal x;
	private BigDecimal y;
	private String description;
	private Object row;

	Point() {}

	public Point(BigDecimal x, BigDecimal y) {
		this.x = x;
		this.y = y;
	}

	public BigDecimal getX() {
		return x;
	}

	void setX(BigDecimal x) {
		this.x = x;
	}

	public BigDecimal getY() {
		return y;
	}

	void setY(BigDecimal y) {
		this.y = y;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Object getRow() {
		return row;
	}

	public void setRow(Object row) {
		this.row = row;
	}

}
