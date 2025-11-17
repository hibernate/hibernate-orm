/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of IntegerVersioned.
 *
 * @author Steve Ebersole
 */
public class IntegerVersioned {
	private Long id;
	private int version = -1;
	private String name;

	public IntegerVersioned() {
	}

	public IntegerVersioned(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public int getVersion() {
		return version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
