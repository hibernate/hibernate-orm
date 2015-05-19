/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;


/**
 * todo: describe IntegerPropertyValue
 *
 * @author Steve Ebersole
 */
public class IntegerPropertyValue implements PropertyValue {
	private Long id;
	private int value;

	public IntegerPropertyValue() {
	}

	public IntegerPropertyValue(int value) {
		this.value = value;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String asString() {
		return Integer.toString( value );
	}
}
