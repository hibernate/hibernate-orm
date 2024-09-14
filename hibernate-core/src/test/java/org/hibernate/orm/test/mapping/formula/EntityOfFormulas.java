/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
