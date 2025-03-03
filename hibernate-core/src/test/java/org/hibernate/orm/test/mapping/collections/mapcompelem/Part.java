/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;



/**
 * @author Gavin King
 */
public class Part {
	private String name;
	private String description;
	Part() {}
	public Part(String n, String pw) {
		name=n;
		description = pw;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String password) {
		this.description = password;
	}
	public boolean equals(Object that) {
		return ( (Part) that ).getName().equals(name);
	}
	public int hashCode() {
		return name.hashCode();
	}
	public String toString() {
		return name + ":" + description;
	}
}
