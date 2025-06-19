/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.Set;

public class A {
	private int id;

	private String data;

	// A 1 - * H
	private Set hs;

	// A 1 - 1 G
	private G g;


	public A() {
		hs = new HashSet();
	}

	public A(String data) {
		this();
		this.data = data;
	}


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setHs(Set hs) {
		this.hs = hs;
	}

	public Set getHs() {
		return hs;
	}

	public void setG(G g) {
		this.g = g;
	}

	public G getG() {
		return g;
	}

	public void addH(H h) {
		hs.add( h );
		h.setA( this );
	}

	public String toString() {
		return "A[" + id + ", " + data + "]";
	}
}
