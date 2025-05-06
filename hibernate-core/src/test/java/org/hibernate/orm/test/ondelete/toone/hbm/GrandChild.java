/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete.toone.hbm;

/**
 * @author Vlad Mihalcea
 */
public class GrandChild {

	private Long id;

	private Child parent;

	public Child getParent() {
		return parent;
	}

	public void setParent(Child parent) {
		this.parent = parent;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
