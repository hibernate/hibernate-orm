/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = IdClassMixedGenerationTests.Animal.class)
class IdClassMixedGenerationTests {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Animal animal = new Animal( 1L );
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
		@Column(name = "sub_id")
		private Long subId;

		Animal(Long subId) {
			this.subId = subId;
		}

		Animal() {
		}
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
