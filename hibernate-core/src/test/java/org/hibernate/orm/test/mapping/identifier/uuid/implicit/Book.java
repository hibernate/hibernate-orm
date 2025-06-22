/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.uuid.implicit;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;

/**
 * @author Steve Ebersole
 */
//tag::example-identifiers-generators-uuid-implicit[]
@Entity
public class Book {
	@Id
	@GeneratedValue
	private UUID id;
	@Basic
	private String name;

	//end::example-identifiers-generators-uuid-implicit[]
	protected Book() {
		// for Hibernate use
	}

	public Book(String name) {
		this.name = name;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

//tag::example-identifiers-generators-uuid-implicit[]
}
//end::example-identifiers-generators-uuid-implicit[]
