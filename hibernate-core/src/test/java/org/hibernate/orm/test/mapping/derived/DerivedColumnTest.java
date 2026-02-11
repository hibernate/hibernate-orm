/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.derived;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.DerivedColumn;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.sql.Types;

@Jpa(annotatedClasses = DerivedColumnTest.Thing.class)
class DerivedColumnTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory();
	}
	@Entity
	@DerivedColumn(name = "computed",
			sqlType = Types.DOUBLE,
			value = "number*(number + 1)")
	static class Thing {
		@GeneratedValue @Id
		long id;
		double number;
	}
}
