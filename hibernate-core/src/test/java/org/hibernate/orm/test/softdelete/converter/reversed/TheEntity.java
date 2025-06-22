/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.converter.reversed;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "the_entity")
//tag::example-soft-delete-reverse[]
@Entity
@SoftDelete(converter = YesNoConverter.class, strategy = SoftDeleteType.ACTIVE)
public class TheEntity {
	// ...
//end::example-soft-delete-reverse[]
	@Id
	private Integer id;
	@Basic
	private String name;

	protected TheEntity() {
		// for Hibernate use
	}

	public TheEntity(Integer id, String name) {
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
