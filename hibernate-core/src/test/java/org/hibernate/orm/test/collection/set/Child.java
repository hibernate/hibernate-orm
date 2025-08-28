/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;


/**
 * todo: describe Child
 *
 * @author Steve Ebersole
 */
public class Child {
	private String name;
	private Parent parent;
	private String description;

	public Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Child child = ( Child ) o;

		if ( description != null ? ! description.equals( child.description ) : child.description != null ) {
			return false;
		}
		if ( ! name.equals( child.name ) ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + ( description != null ? description.hashCode() : 0 );
		return result;
	}
}
