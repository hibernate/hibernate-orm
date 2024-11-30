/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;


/**
 *
 * @author Gail Badner
 */
public class ChildValue implements Child {
	private String name;

	public ChildValue() {
	}

	public ChildValue(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(Object otherChild) {
		if ( this == otherChild ) {
			return true;
		}
		if ( !( otherChild instanceof ChildValue ) ) {
			return false;
		}
		return name.equals( ( ( ChildValue ) otherChild ).name );
	}

	public int hashCode() {
		return name.hashCode();
	}
}
