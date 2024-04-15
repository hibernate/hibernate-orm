/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.io.Serializable;

/**
 * A mutable (as in non-`@Immutable`) value.  Mainly used for testing
 * JPA AttributeConverter support for mutable domain values in regards
 * to caching, dirty-checking, etc
 */
public class MutableValue implements Serializable {
	private String state;

	public MutableValue() {
	}

	public MutableValue(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
