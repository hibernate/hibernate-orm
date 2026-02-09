/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = EmbeddedIdMixedGenerationTests.Animal.class)
class EmbeddedIdMixedGenerationTests {

	@Test void test(EntityManagerFactoryScope scope) {
		var id = scope.fromTransaction( em -> {
			Animal animal = new Animal();
			em.persist( animal );
			return animal;
		} ).idWithSubId;
		scope.inTransaction( em -> {
			Animal animal = em.find( Animal.class, id );
			assertEquals( 1, animal.idWithSubId.id );
			assertEquals( 1, animal.idWithSubId.subId );
		} );

		var id2 = scope.fromTransaction( em -> {
			Animal animal = new Animal();
			animal.idWithSubId = new IdWithSubId();
			em.persist( animal );
			return animal;
		} ).idWithSubId;
		scope.inTransaction( em -> {
			Animal animal = em.find( Animal.class, id2 );
			assertEquals( 2, animal.idWithSubId.id );
			assertEquals( 2, animal.idWithSubId.subId );
		} );
	}

	@Entity
	@Table(name = "animals")
	static class Animal {
		@EmbeddedId IdWithSubId idWithSubId;
	}
	static class IdWithSubId {
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@Column(name = "sub_id")
		private Long subId;
	}
}
