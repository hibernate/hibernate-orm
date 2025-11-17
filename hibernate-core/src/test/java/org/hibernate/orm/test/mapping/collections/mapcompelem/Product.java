/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gavin King
 */
public class Product {
	private String name;
	private Map parts = new HashMap();
	Product() {}
	public Product(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map getParts() {
		return parts;
	}
	public void setParts(Map users) {
		this.parts = users;
	}
}
