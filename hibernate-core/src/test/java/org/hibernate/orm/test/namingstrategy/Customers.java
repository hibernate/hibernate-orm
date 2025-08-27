/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;


/**
 * @author Emmanuel Bernard
 */
public class Customers implements java.io.Serializable {
	private int id;
	private String specified_column;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSpecified_column() {
		return specified_column;
	}

	public void setSpecified_column(String specified_column) {
		this.specified_column = specified_column;
	}
}
