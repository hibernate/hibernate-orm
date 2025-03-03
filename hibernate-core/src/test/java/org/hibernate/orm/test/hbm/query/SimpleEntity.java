/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.NamedQuery;

/**
 * @author Steve Ebersole
 */
@Entity
@NamedQuery(name = SimpleEntity.FIND_ALL, query = "from SimpleEntity")
public class SimpleEntity {
	public static final String FIND_ALL = "SimpleEntity.findAll";

	@Id
	private Integer id;
	@Basic
	private String name;

	protected SimpleEntity() {
		// for Hibernate use
	}

	public SimpleEntity(Integer id, String name) {
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
