/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

/**
 * Entity used in <code>org.hibernate.orm.test.jpa.criteria.basic.NegatedInPredicateTest</code>.
 *
 * @author Mike Mannion
 */
@Entity
public class Foo {

	@Id
	Long id;

	@ManyToOne
	@JoinColumns({
			@JoinColumn(name = "FK_CODE", referencedColumnName = "CODE"),
			@JoinColumn(name = "FK_CONTEXT", referencedColumnName = "CONTEXT")
	})
	FooType fooType;

	public Foo() {
		// For JPA
	}

	public Foo(Long id, FooType fooType) {
		this.id = id;
		this.fooType = fooType;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;

		Foo customer = (Foo) o;
		return Objects.equals(id, customer.id) && Objects.equals(fooType, customer.fooType);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(id);
		result = 31 * result + Objects.hashCode(fooType);
		return result;
	}
}
