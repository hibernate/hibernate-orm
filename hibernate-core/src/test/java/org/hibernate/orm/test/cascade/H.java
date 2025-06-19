/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.Set;

public class H {
	private int id;

	private String data;

	private A a;

	// G * <-> * H
	private Set gs;

	public H() {
		this( null );
	}

	public H(String data) {
		this.data = data;
		gs = new HashSet();
	}

	public int getId() {
		return id;
	}

	private void setId(int id) {
		this.id = id;
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

	public Set getGs() {
		return gs;
	}

	public void setGs(Set gs) {
		this.gs = gs;
	}
}
