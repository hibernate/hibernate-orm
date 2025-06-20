/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import org.hibernate.type.descriptor.JdbcBindingLogging;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.Assert.assertTrue;

@MessageKeyInspection(
		logger = @Logger( loggerName = JdbcBindingLogging.NAME ),
		messageKey = "binding parameter"
)
@DomainModel( annotatedClasses = VarcharEnumTypeTest.Person.class )
@SessionFactory
public class VarcharEnumTypeTest {
	@BeforeEach
	protected void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Person person = Person.person( Gender.MALE, HairColor.BROWN );
					session.persist( person );
					session.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
					session.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
					session.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey("HHH-12978")
	public void testEnumAsBindParameterAndExtract(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "select p.id from Person p where p.id = :id", Long.class )
							.setParameter( "id", 1L )
							.list();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);

		loggingWatcher.reset();

		scope.inTransaction(
				(session) -> {
					final String qry = "select p.gender from Person p where p.gender = :gender and p.hairColor = :hairColor";
					session.createQuery( qry, Gender.class )
							.setParameter( "gender", Gender.MALE )
							.setParameter( "hairColor", HairColor.BROWN )
							.getSingleResult();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey("HHH-10282")
	public void hqlTestEnumShortHandSyntax(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					session.createQuery(
							"select id from Person where originalHairColor = BLONDE")
							.getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey("HHH-10282")
	public void hqlTestEnumQualifiedShortHandSyntax(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		final String qry = "select id from Person where originalHairColor = HairColor.BLONDE";
		scope.inTransaction(
				(session) -> {
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey("HHH-10282")
	public void hqlTestEnumShortHandSyntaxInPredicate(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					final String qry = "select id from Person where originalHairColor in (BLONDE, BROWN)";
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}

	@Test
	@JiraKey("HHH-10282")
	public void hqlTestEnumQualifiedShortHandSyntaxInPredicate(SessionFactoryScope scope, MessageKeyWatcher loggingWatcher) {
		scope.inTransaction(
				(session) -> {
					final String qry = "select id from Person where originalHairColor in (HairColor.BLONDE, HairColor.BROWN)";
					session.createQuery( qry ).getResultList();

					assertTrue( loggingWatcher.wasTriggered() );
				}
		);
	}


	@Test
	@JiraKey("HHH-16739")
	public void testCompareEnumParameterWithDifferentTypedExpressions(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					s.createQuery( "select p.id from Person p where p.gender = :gender and :gender = 'MALE'", Long.class )
							.setParameter( "gender", Gender.MALE )
							.getResultList();
					s.createQuery( "select p.id from Person p where p.gender = :gender and :gender = org.hibernate.orm.test.mapping.converted.enums.Gender.MALE", Long.class )
							.setParameter( "gender", Gender.MALE )
							.getResultList();

					s.createQuery( "select p.id from Person p where :gender = org.hibernate.orm.test.mapping.converted.enums.Gender.MALE and p.gender = :gender", Long.class )
							.setParameter( "gender", Gender.MALE )
							.getResultList();

					s.createQuery( "select p.id from Person p where :gender = 'MALE' and p.gender = :gender", Long.class )
							.setParameter( "gender", Gender.MALE )
							.getResultList();

					s.createQuery( "select p.id from Person p where :gender = 'MALE' or :gender = 'FEMALE' and p.gender = :gender", Long.class )
							.setParameter( "gender", Gender.MALE )
							.getResultList();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Enumerated(EnumType.STRING)
		private Gender gender;

		@Enumerated(EnumType.STRING)
		private HairColor hairColor;

		@Enumerated(EnumType.STRING)
		private HairColor originalHairColor;

		public static Person person(Gender gender, HairColor hairColor) {
			Person person = new Person();
			person.setGender( gender );
			person.setHairColor( hairColor );
			return person;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public HairColor getHairColor() {
			return hairColor;
		}

		public void setHairColor(HairColor hairColor) {
			this.hairColor = hairColor;
		}

		public HairColor getOriginalHairColor() {
			return originalHairColor;
		}

		public void setOriginalHairColor(HairColor originalHairColor) {
			this.originalHairColor = originalHairColor;
		}
	}
}
