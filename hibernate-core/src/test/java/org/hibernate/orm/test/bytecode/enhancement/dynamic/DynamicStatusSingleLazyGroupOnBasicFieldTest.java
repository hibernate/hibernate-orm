/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dynamic;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@JiraKey("HHH-15186")
@BytecodeEnhanced
@DomainModel(
		annotatedClasses = {
				DynamicStatusSingleLazyGroupOnBasicFieldTest.Person.class
		}
)
@SessionFactory
public class DynamicStatusSingleLazyGroupOnBasicFieldTest {

	private static final Long PERSON_ID = 1L;
	private static final String PERSON_NAME = "And";
	private static final String PERSON_SURNAME = "Bor";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( PERSON_ID, PERSON_SURNAME, PERSON_NAME );
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.findEntityDescriptor( Person.class );
		assertThat( persister.isDynamicUpdate() ).isFalse();
	}

	@Test
	public void testUpdateSurname(SessionFactoryScope scope) {
		String updatedSurname = PERSON_SURNAME + "_1";
		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person ).isNotNull();
					assertThat( Hibernate.isPropertyInitialized( person, "surname" ) ).isFalse();
					assertThat( Hibernate.isPropertyInitialized( person, "name" ) ).isTrue();
					person.setSurname( updatedSurname );
				}
		);

		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person.getSurname() ).isEqualTo( updatedSurname );
					assertThat( person.getName() ).isEqualTo( PERSON_NAME );
				}
		);
	}

	@Test
	public void testUpdateName(SessionFactoryScope scope) {
		String updatedName = PERSON_NAME + "_1";
		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person ).isNotNull();
					assertThat( Hibernate.isPropertyInitialized( person, "surname" ) ).isFalse();
					assertThat( Hibernate.isPropertyInitialized( person, "name" ) ).isTrue();
					person.setName( updatedName );
				}
		);

		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person.getSurname() ).isEqualTo( PERSON_SURNAME );
					assertThat( person.getName() ).isEqualTo( updatedName );
				}
		);
	}

	@Test
	public void testUpdateNameAndSurname(SessionFactoryScope scope) {
		String updatedName = PERSON_NAME + "_1";
		String updatedSurname = PERSON_SURNAME + "_1";

		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person ).isNotNull();
					assertThat( Hibernate.isPropertyInitialized( person, "surname" ) ).isFalse();
					assertThat( Hibernate.isPropertyInitialized( person, "name" ) ).isTrue();
					person.setName( updatedName );
					person.setSurname( updatedSurname );
				}
		);

		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person.getSurname() ).isEqualTo( updatedSurname );
					assertThat( person.getName() ).isEqualTo( updatedName );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@Basic(fetch = FetchType.LAZY)
		@LazyGroup("group1")
		private String surname;

		private String name;

		public Person() {
		}

		public Person(Long id, String surname, String name) {
			this.id = id;
			this.surname = surname;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
