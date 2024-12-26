/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.pack.scan;

/**
 * @author Steve Ebersole
 */
public class CheeseSpread {
	private Integer id;
	private String name;

	protected CheeseSpread() {
		// for Hibernate use
	}

	public CheeseSpread(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
