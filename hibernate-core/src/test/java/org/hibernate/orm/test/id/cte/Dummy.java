/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity(name = "Dummy")
class Dummy {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dummy_seq")
	@SequenceGenerator(name = "dummy_seq", sequenceName = "dummy_seq", allocationSize = 5)
	private Long id;

	private String name;

	public Dummy() {
	}

	public Dummy(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
