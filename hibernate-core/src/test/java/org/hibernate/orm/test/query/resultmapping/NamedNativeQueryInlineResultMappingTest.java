/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(annotatedClasses = NamedNativeQueryInlineResultMappingTest.Person.class)
class NamedNativeQueryInlineResultMappingTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new Person( 1, "Claude" ) ) );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	void mapsEntitiesMember(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Person result = entityManager.createNamedQuery( "Person.inlineEntity", Person.class ).getSingleResult();

			assertThat( result.id ).isEqualTo( 1 );
			assertThat( result.name ).isEqualTo( "Claude" );
		} );
	}

	@Test
	void mapsClassesMember(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final PersonSummary result =
					entityManager.createNamedQuery( "Person.inlineConstructor", PersonSummary.class ).getSingleResult();

			assertThat( result.id ).isEqualTo( 1 );
			assertThat( result.name ).isEqualTo( "Claude" );
		} );
	}

	@Test
	void mapsColumnsMember(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final String result = entityManager.createNamedQuery( "Person.inlineScalar", String.class ).getSingleResult();

			assertThat( result ).isEqualTo( "Claude" );
		} );
	}

	@Entity(name = "InlineResultPerson")
	@Table(name = "NNQ_PERSON")
	@NamedNativeQueries({
			@NamedNativeQuery(
					name = "Person.inlineEntity",
					query = "select p.id as id, p.person_name as person_name from NNQ_PERSON p",
					entities = @EntityResult(
							entityClass = Person.class,
							fields = {
									@FieldResult(name = "id", column = "id"),
									@FieldResult(name = "name", column = "person_name")
							}
					)
			),
			@NamedNativeQuery(
					name = "Person.inlineConstructor",
					query = "select p.id as id, p.person_name as person_name from NNQ_PERSON p",
					classes = @ConstructorResult(
							targetClass = PersonSummary.class,
							columns = {
									@ColumnResult(name = "id", type = Integer.class),
									@ColumnResult(name = "person_name", type = String.class)
							}
					)
			),
			@NamedNativeQuery(
					name = "Person.inlineScalar",
					query = "select p.person_name as person_name from NNQ_PERSON p",
					columns = @ColumnResult(name = "person_name", type = String.class)
			)
	})
	static class Person {
		@Id
		Integer id;

		@Column(name = "person_name")
		String name;

		Person() {
		}

		Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class PersonSummary {
		final Integer id;
		final String name;

		public PersonSummary(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
