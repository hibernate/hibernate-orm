/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractNameable {
	private String name;

	protected AbstractNameable() {
	}

	protected AbstractNameable(String name) {
		this.name = name;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
