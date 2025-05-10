/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.Set;

public class G {

	private int id;

	private String data;

	// A 1 <-> 1 G
	private A a;

	// G * <-> * H
	private Set hs;


	public G() {
		this( null );
	}

	public G(String data) {
		this.data = data;
		hs = new HashSet();
	}


	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public Set getHs() {
		return hs;
	}

	public void setHs(Set s) {
		hs = s;
	}

	int getId() {
		return id;
	}

	private void setId(int id) {
		this.id = id;
	}

}
