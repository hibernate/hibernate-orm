/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.uuid.implicit2;

import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::example-identifiers-generators-uuid-implicit[]
@Entity
public class Book {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
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
