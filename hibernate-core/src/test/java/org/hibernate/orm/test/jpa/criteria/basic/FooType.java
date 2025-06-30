/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity used in <code>org.hibernate.orm.test.jpa.criteria.basic.NegatedInPredicateTest</code>.
 *
 * @author Mike Mannion
 */
@Entity
public class FooType {
	@Id
	@Column(name = "CODE")
	String code;

	@Column(name = "CONTEXT")
	String context;

	public FooType() {
		// For JPA
	}

	FooType(String code, String context) {
		this.code = code;
		this.context = context;
	}
}
