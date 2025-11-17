/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.LoggingInspections;
import org.hibernate.testing.orm.junit.LoggingInspectionsScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.JdbcBindingLogging;
import org.hibernate.type.descriptor.JdbcExtractingLogging;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Brett Meyer
 */
@DomainModel(xmlMappings = {"org/hibernate/orm/test/mapping/converted/enums/Person.hbm.xml" })
@SessionFactory
@ServiceRegistry(settings = {@Setting(name = Environment.PREFER_NATIVE_ENUM_TYPES, value = "false")})
@LoggingInspections(
		messages = {
				@LoggingInspections.Message(
						messageKey = "binding parameter",
						loggers = @org.hibernate.testing.orm.junit.Logger(loggerName = JdbcBindingLogging.NAME)
				),
				@LoggingInspections.Message(
						messageKey = "extracted value",
						loggers = @org.hibernate.testing.orm.junit.Logger(loggerName = JdbcExtractingLogging.NAME)
				)
		}
)
public class EnumTypeTest {

	private Person person;

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			this.person = Person.person( Gender.MALE, HairColor.BROWN );
			s.persist( person );
			s.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
			s.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
			s.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
		} );
	}

	@AfterEach
	protected void cleanupTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-8153")
	public void hbmEnumTypeTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
					assertEquals( 2, getNumberOfPersonByGender( s, Gender.MALE ) );
					assertEquals( 1, getNumberOfPersonByGenderAndHairColor( s, Gender.MALE, HairColor.BROWN ) );
					assertEquals( 2, getNumberOfPersonByGender( s, Gender.FEMALE ) );
					assertEquals( 1, getNumberOfPersonByGenderAndHairColor( s, Gender.FEMALE, HairColor.BROWN ) );
				}
		);
	}

	private int getNumberOfPersonByGender(Session session, Gender value) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
		Root<Person> root = criteria.from( Person.class );
		criteria.where( criteriaBuilder.equal( root.get( "gender" ), value ) );
		return session.createQuery( criteria ).list().size();
	}

	private int getNumberOfPersonByGenderAndHairColor(Session session, Gender gender, HairColor hairColor) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
		Root<Person> root = criteria.from( Person.class );
		criteria.where( criteriaBuilder.and(
				criteriaBuilder.equal( root.get( "gender" ), gender ),
				criteriaBuilder.equal( root.get( "hairColor" ), hairColor )
		) );
		return session.createQuery( criteria ).list().size();
	}

	@Test
	@JiraKey(value = "HHH-12978")
	public void testEnumAsBindParameterAndExtract(LoggingInspectionsScope loggingScope, SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			loggingScope.resetWatchers();

			s.createQuery( "select p.id from Person p where p.id = :id", Long.class )
					.setParameter( "id", person.getId() )
					.getSingleResult();

			assertTrue( loggingScope.getWatcher( "binding parameter", JdbcBindingLogging.NAME ).wasTriggered() );
			assertTrue( loggingScope.getWatcher( "extracted value", JdbcExtractingLogging.NAME ).wasTriggered() );
		} );

		scope.inTransaction( s -> {
			loggingScope.resetWatchers();

			s.createQuery(
					"select p.gender from Person p where p.gender = :gender and p.hairColor = :hairColor",
					Gender.class
			)
					.setParameter( "gender", Gender.MALE )
					.setParameter( "hairColor", HairColor.BROWN )
					.getSingleResult();

			assertTrue( loggingScope.getWatcher( "binding parameter", JdbcBindingLogging.NAME ).wasTriggered() );
			assertTrue( loggingScope.getWatcher( "extracted value", JdbcExtractingLogging.NAME ).wasTriggered() );
		} );
	}
}
