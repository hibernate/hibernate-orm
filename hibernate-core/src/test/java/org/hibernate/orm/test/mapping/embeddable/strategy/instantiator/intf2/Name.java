/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf2;

/**
 * @author Steve Ebersole
 */
public class Name {
	private final String first;
	private final String last;

	public static Name make(String first, String last) {
		return new Name( first, last );
	}

	private Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	String getFirstName() {
		return first;
	}

	String getLastName() {
		return last;
	}
}
