/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = MoreIdClassMixedGenerationTests.Animal.class)
class MoreIdClassMixedGenerationTests {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Animal animal = new Animal();
			em.persist( animal );
			assertEquals( 1, animal.id );
			assertEquals( 1, animal.subId );
		} );
		scope.inTransaction( em -> {
			Animal animal = em.find( Animal.class, new IdWithSubId( 1L, 1L ) );
			assertEquals( 1, animal.id );
			assertEquals( 1, animal.subId );
		} );
	}

	@Entity
	@Table(name = "animals")
	@IdClass(IdWithSubId.class)
	static class Animal {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@Column(name = "sub_id")
		private Long subId;
	}
	static class IdWithSubId {
		private Long id;
		private Long subId;
		IdWithSubId() {}
		IdWithSubId(long l, long l1) {
			this.id = l;
			this.subId = l1;
		}
	}
}
