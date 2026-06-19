/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.recursivecomponent;

public class MyComponent {
	private String name;
	private int count;
	private MyComponent subcomponent;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public MyComponent getSubcomponent() {
		return subcomponent;
	}

	public void setSubcomponent(MyComponent subcomponent) {
		this.subcomponent = subcomponent;
	}
}
