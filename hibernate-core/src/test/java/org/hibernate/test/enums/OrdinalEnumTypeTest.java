/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihacea
 */
public class OrdinalEnumTypeTest extends BaseCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule binderLogInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			BasicBinder.class.getName()
	) );

	@Rule
	public LoggerInspectionRule extractorLogInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			BasicExtractor.class.getName()
	) );

	private Person person;

	private Triggerable binderTriggerable;

	private Triggerable extractorTriggerable;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Override
	protected void prepareTest() {
		doInHibernate( this::sessionFactory, s -> {
			this.person = Person.person( Gender.MALE, HairColor.BROWN );
			s.persist( person );
			s.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
			s.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
			s.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
		} );

		binderTriggerable = binderLogInspection.watchForLogMessages( "binding parameter" );
		extractorTriggerable = extractorLogInspection.watchForLogMessages( "extracted value" );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12978")
	public void testEnumAsBindParameterAndExtract() {
		doInHibernate( this::sessionFactory, s -> {
			binderTriggerable.reset();
			extractorTriggerable.reset();

			s.createQuery( "select p.id from Person p where p.id = :id", Long.class )
					.setParameter( "id", person.getId() )
					.getSingleResult();

			assertTrue( binderTriggerable.wasTriggered() );
			assertTrue( extractorTriggerable.wasTriggered() );
		} );

		doInHibernate( this::sessionFactory, s -> {
			binderTriggerable.reset();
			extractorTriggerable.reset();

			s.createQuery(
					"select p.gender from Person p where p.gender = :gender and p.hairColor = :hairColor",
					Gender.class
			)
					.setParameter( "gender", Gender.MALE )
					.setParameter( "hairColor", HairColor.BROWN )
					.getSingleResult();

			assertTrue( binderTriggerable.wasTriggered() );
			assertTrue( extractorTriggerable.wasTriggered() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		private Gender gender;

		@Enumerated(EnumType.ORDINAL)
		private HairColor hairColor;

		@Enumerated(EnumType.ORDINAL)
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
