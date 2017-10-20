/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import javax.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
@SuppressWarnings("unused")
public class Component {
	private String basicString;
	private Integer basicInteger;
	private Long basicLong;
	private int basicPrimitiveInt;
	private Nested nested;

	@Embeddable
	public static class Nested {
		private String nestedValue;

		public String getNestedValue() {
			return nestedValue;
		}

		public void setNestedValue(String nestedValue) {
			this.nestedValue = nestedValue;
		}
	}

	public String getBasicString() {
		return basicString;
	}

	public void setBasicString(String basicString) {
		this.basicString = basicString;
	}

	public Integer getBasicInteger() {
		return basicInteger;
	}

	public void setBasicInteger(Integer basicInteger) {
		this.basicInteger = basicInteger;
	}

	public Long getBasicLong() {
		return basicLong;
	}

	public void setBasicLong(Long basicLong) {
		this.basicLong = basicLong;
	}

	public int getBasicPrimitiveInt() {
		return basicPrimitiveInt;
	}

	public void setBasicPrimitiveInt(int basicPrimitiveInt) {
		this.basicPrimitiveInt = basicPrimitiveInt;
	}

	public Nested getNested() {
		return nested;
	}

	public void setNested(Nested nested) {
		this.nested = nested;
	}
}
