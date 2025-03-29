/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

public class EntityOfFormulas {
	private Integer id;
	private Integer realValue;
	private String stringFormula;
	private Integer integerFormula;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getRealValue() {
		return realValue;
	}

	public void setRealValue(Integer realValue) {
		this.realValue = realValue;
	}

	public String getStringFormula() {
		return stringFormula;
	}

	public void setStringFormula(String stringFormula) {
		this.stringFormula = stringFormula;
	}

	public Integer getIntegerFormula() {
		return integerFormula;
	}

	public void setIntegerFormula(Integer integerFormula) {
		this.integerFormula = integerFormula;
	}
}
