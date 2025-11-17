/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.checkconstraint.table;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "TEST_ENTITY_1")
@Table(
		name = "TEST_ENTITY_TABLE",
		check = {
				@CheckConstraint(
						name = "TABLE_CONSTRAINT",
						constraint = TableCheckConstraintTest.CONSTRAINTS
				)
		}
)
public class TestEntity {
	@Id
	private Long id;

	@Column(name = "NAME_COLUMN")
	private String name;

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
