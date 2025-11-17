/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "simple")
//tag::example-soft-delete-basic[]
@Entity(name = "SimpleEntity")
@SoftDelete(columnName = "removed", converter = YesNoConverter.class)
public class SimpleEntity {
	// ...
//end::example-soft-delete-basic[]
	@Id
	private Integer id;
	@NaturalId
	private String name;

	public SimpleEntity() {
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
//tag::example-soft-delete-basic[]
}
//end::example-soft-delete-basic[]
