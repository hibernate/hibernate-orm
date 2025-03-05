/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Detail implements Serializable {

	private Root root;
	private int i;
	private Set details = new HashSet();
	private int x;

	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}

	public Root getRoot() {
		return root;
	}

	public void setRoot(Root root) {
		this.root = root;
	}

	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	/**
	 * Returns the details.
	 * @return Set
	 */
	public Set getSubDetails() {
		return details;
	}

	/**
	 * Sets the details.
	 * @param details The details to set
	 */
	public void setSubDetails(Set details) {
		this.details = details;
	}

}
