/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.archive;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

/**
 * @author Steve Ebersole
 */
@Entity
public class SecondEntity {
	@Id
	private Integer id;
	@Basic
	private String name;

	protected SecondEntity() {
		// for Hibernate use
	}

	public SecondEntity(Integer id, String name) {
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
