/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
@SuppressWarnings("unused")
public class Component {

	// alphabetical
	private Integer basicInteger;
	private Long basicLong;
	private int basicPrimitiveInt;
	private String basicString;
	private Nested nested;

	@Embeddable
	public static class Nested {

		// alphabetical
		private String nestedValue;
		private String secondNestedValue;

		public Nested() {
		}

		public Nested(String nestedValue) {
			this.nestedValue = nestedValue;
		}

		public Nested(String nestedValue, String secondNestedValue) {
			this.nestedValue = nestedValue;
			this.secondNestedValue = secondNestedValue;
		}

		public String getNestedValue() {
			return nestedValue;
		}

		public void setNestedValue(String nestedValue) {
			this.nestedValue = nestedValue;
		}

		public String getSecondNestedValue() {
			return secondNestedValue;
		}

		public void setSecondNestedValue(String secondNestedValue) {
			this.secondNestedValue = secondNestedValue;
		}
	}

	public Component() {
	}

	public Component(
			String basicString,
			Integer basicInteger,
			Long basicLong,
			int basicPrimitiveInt,
			Nested nested) {
		this.basicString = basicString;
		this.basicInteger = basicInteger;
		this.basicLong = basicLong;
		this.basicPrimitiveInt = basicPrimitiveInt;
		this.nested = nested;
	}

	public Component(
			Integer basicInteger,
			Long basicLong,
			int basicPrimitiveInt,
			String basicString,
			Nested nested) {
		this.basicInteger = basicInteger;
		this.basicLong = basicLong;
		this.basicPrimitiveInt = basicPrimitiveInt;
		this.basicString = basicString;
		this.nested = nested;
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
