/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.original;


/**
 * @author Gavin King
 */
public class Permission {
	private String type;
	Permission() {}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Permission(String type) {
		this.type = type;
	}
	public boolean equals(Object that) {
		if ( !(that instanceof Permission) ) return false;
		Permission p = (Permission) that;
		return this.type.equals(p.type);
	}
	public int hashCode() {
		return type.hashCode();
	}
}
