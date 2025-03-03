/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


public class Many {
	Long key;
	One one;
	private int x;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public void setKey(Long key) {
		this.key = key;
	}

	public Long getKey() {
		return this.key;
	}

	public void setOne(One one) {
		this.one = one;
	}

	public One getOne() {
		return this.one;
	}
}
