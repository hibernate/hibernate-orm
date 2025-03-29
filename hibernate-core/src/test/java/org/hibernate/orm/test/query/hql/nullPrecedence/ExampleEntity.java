/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.nullPrecedence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity( name = "ExampleEntity" )
@Table( name = "ExampleEntity" )
public class ExampleEntity {
	@Id
	private Long id;

	private String name;

	public ExampleEntity() {
	}

	public ExampleEntity(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
