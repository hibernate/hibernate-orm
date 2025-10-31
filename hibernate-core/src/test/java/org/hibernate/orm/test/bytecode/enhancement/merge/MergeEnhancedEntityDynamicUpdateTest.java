/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Luis Barreiro
 */
@JiraKey(value = "HHH-11459")
@DomainModel(
		annotatedClasses = {
				MergeEnhancedEntityDynamicUpdateTest.Person.class,
				MergeEnhancedEntityDynamicUpdateTest.PersonAddress.class,
				MergeEnhancedEntityDynamicUpdateTest.NullablePerson.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MergeEnhancedEntityDynamicUpdateTest {
	private Person person;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		person = new Person( 1L, "Sam" );
		scope.inTransaction( s -> {
			s.persist( person );
		} );
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Person entity = s.find( Person.class, 1L );
			entity.name = "John";
			try {
				s.merge( entity );
			}
			catch (RuntimeException e) {
				fail( "Enhanced entity can't be merged: " + e.getMessage() );
			}
		} );
	}

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Person entity = s.find( Person.class, 1L );
			entity.name = "John";

			s.refresh( entity );

//            try {
//                s.refresh( entity );
//            } catch ( RuntimeException e ) {
//                fail( "Enhanced entity can't be refreshed: " + e.getMessage() );
//            }
		} );
	}

	@Test
	public void testMergeWithNullValues(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			NullablePerson nullablePerson = new NullablePerson( 1L, "Sam", 100 );
			s.persist( nullablePerson );
		} );
		scope.inTransaction( em -> {
			NullablePerson updated = em.find( NullablePerson.class, 1L );
			assertThat( updated.name ).isEqualTo( "Sam" );
			assertThat( updated.number ).isEqualTo( 100 );
		} );

		// only some properties are null
		scope.inTransaction( em -> {
			NullablePerson nullablePerson = new NullablePerson( 1L, "Joe", null );
			em.merge( nullablePerson );
		} );
		scope.inTransaction( em -> {
			NullablePerson updated = em.find( NullablePerson.class, 1L );
			assertThat( updated.name ).isEqualTo( "Joe" );
			assertThat( updated.number ).isNull();
		} );

		// all properties are null:
		scope.inTransaction( em -> {
			NullablePerson nullablePerson = new NullablePerson( 1L, null, null );
			em.merge( nullablePerson );
		} );
		scope.inTransaction( em -> {
			NullablePerson updated = em.find( NullablePerson.class, 1L );
			assertThat( updated.name ).isNull();
			assertThat( updated.number ).isNull();
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	// --- //

	@Entity
	@Table(name = "PERSON")
	@DynamicUpdate
	@DynamicInsert
	static class Person {

		@Id
		Long id;

		@Column(name = "name", length = 10, nullable = false)
		String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true, cascade = CascadeType.ALL)
		List<PersonAddress> details = new ArrayList<>();

		Person() {
		}

		Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(name = "PERSON_ADDRESS")
	@DynamicUpdate
	@DynamicInsert
	static class PersonAddress {

		@Id
		Long id;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		Person parent;
	}

	@Entity(name = "NullablePerson")
	@Table(name = "NULLABLE_PERSON")
	@DynamicUpdate
	@DynamicInsert
	static class NullablePerson {

		@Id
		Long id;

		@Column
		String name;

		@Column(name = "NUMBER_COLUMN")
		Integer number;

		NullablePerson() {
		}

		NullablePerson(Long id, String name, Integer number) {
			this.id = id;
			this.name = name;
			this.number = number;
		}
	}
}
