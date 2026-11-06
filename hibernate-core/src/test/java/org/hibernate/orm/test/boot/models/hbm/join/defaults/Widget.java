/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.join.defaults;

/**
 * Entity mapped entirely through {@code orm.xml}; its {@code <secondary-table/>}
 * declares neither {@code owned} nor {@code optional}, so both must fall back to
 * the {@link org.hibernate.annotations.SecondaryRow} annotation defaults
 * ({@code owned=true}, {@code optional=true}).
 */
public class Widget {
	private Integer id;
	private String name;
	private String extra;

	protected Widget() {
		// for Hibernate use
	}

	public Widget(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
