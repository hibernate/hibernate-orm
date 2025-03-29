/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Group {
	private Long id;
	private String name;

	/**
	 * for persistence
	 */
	Group() {
	}

	public Group(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
