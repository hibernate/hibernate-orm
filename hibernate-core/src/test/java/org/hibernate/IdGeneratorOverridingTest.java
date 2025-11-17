/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = {IdGeneratorOverridingTest.A.class, IdGeneratorOverridingTest.B.class})
public class IdGeneratorOverridingTest {

	@FailureExpected(reason = "We don't (yet) allow overriding of ids declared by mapped superclasses")
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new B() ) );
	}

	@MappedSuperclass
	static abstract class A {
		private Long id;

		@Id
		@GeneratedValue(generator = "a_sequence")
		@SequenceGenerator(name = "a_sequence", sequenceName = "a_sequence")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	static class B extends A {
		private Long id;

		@Id
		@GeneratedValue(generator = "b_sequence")
		@SequenceGenerator(name = "b_sequence", sequenceName = "b_sequence")
		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}
	}
}
