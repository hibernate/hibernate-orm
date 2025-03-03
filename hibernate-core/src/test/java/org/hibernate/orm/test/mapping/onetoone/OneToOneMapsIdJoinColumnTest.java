/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				OneToOneMapsIdJoinColumnTest.Person.class,
				OneToOneMapsIdJoinColumnTest.PersonDetails.class
		},
		useCollectingStatementInspector = true
)
public class OneToOneMapsIdJoinColumnTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			Person person = new Person( "ABC-123" );

			PersonDetails details = new PersonDetails();
			details.setNickName( "John Doe" );

			person.setDetails( details );
			entityManager.persist( person );

		} );
	}

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		statementInspector.clear();
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find( Person.class, "ABC-123" );
			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

			statementInspector.clear();

			PersonDetails details = entityManager.find( PersonDetails.class, "ABC-123" );
			statementInspector.assertExecutedCount( 0 );

			assertSame( details.getPerson(), person );
			statementInspector.assertExecutedCount( 0 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private String id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST, optional = false)
		private PersonDetails details;

		public Person() {
		}

		public Person(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setDetails(PersonDetails details) {
			this.details = details;
			details.setPerson( this );
		}
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails {

		@Id
		private String id;

		private String nickName;

		@OneToOne
		@MapsId
		@JoinColumn(name = "person_id")
		private Person person;

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

}
