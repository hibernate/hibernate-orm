/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide.tooling;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Table(name = "customers")
//tag::tooling-modelgen-model[]
@Entity
public class Customer {
	@Id
	private Integer id;
	@Basic
	private String name;

	// getters and setters omitted for brevity
//end::tooling-modelgen-model[]

	private Customer() {
		// for Hibernate use
	}

	public Customer(Integer id, String name) {
		this.id = id;
		this.name = name;
	}
//tag::tooling-modelgen-model[]
}
//end::tooling-modelgen-model[]
