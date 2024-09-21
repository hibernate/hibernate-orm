/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.custom;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

@Entity(name = "TheEntity")
@Table(name = "TheEntity")
public class TheEntity {
	@Id
	@Sequence( name = "seq1" )
	public Integer id;
	@Basic
	public String name;

	private TheEntity() {
		// for Hibernate use
	}

	public TheEntity(String name) {
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
