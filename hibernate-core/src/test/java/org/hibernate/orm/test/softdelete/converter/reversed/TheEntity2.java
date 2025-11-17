/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.converter.reversed;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "the_entity2")
//tag::example-soft-delete-reverse[]
@Entity
@SoftDelete(strategy = SoftDeleteType.ACTIVE)
public class TheEntity2 {
	// ...
//end::example-soft-delete-reverse[]
	@Id
	private Integer id;
	@Basic
	private String name;

	protected TheEntity2() {
		// for Hibernate use
	}

	public TheEntity2(Integer id, String name) {
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

//tag::example-soft-delete-reverse[]
}
//end::example-soft-delete-reverse[]
