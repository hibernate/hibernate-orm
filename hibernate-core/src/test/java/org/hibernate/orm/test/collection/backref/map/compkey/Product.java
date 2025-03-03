/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.backref.map.compkey;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Product implementation
 *
 * @author Steve Ebersole
 */
public class Product implements Serializable {
	private String name;
	private Map parts = new HashMap();

	public Product() {
	}

	public Product(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Map getParts() {
		return parts;
	}

	public void setParts(Map parts) {
		this.parts = parts;
	}
}
