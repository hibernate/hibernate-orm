/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.JdbcBindingLogging;
import org.hibernate.type.descriptor.JdbcExtractingLogging;

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
 * @author Brett Meyer
 */
public class EnumTypeTest extends BaseCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule binderLogInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			JdbcBindingLogging.NAME
	) );

	@Rule
	public LoggerInspectionRule extractorLogInspection = new LoggerInspectionRule( Logger.getMessageLogger(
			CoreMessageLogger.class,
			JdbcExtractingLogging.NAME
	) );

	private Person person;

	private Triggerable binderTriggerable;

	private Triggerable extractorTriggerable;

	@Override
	protected String getBaseForMappings() {
		return "";
	}

	protected String[] getMappings() {
		return new String[] { "org/hibernate/orm/test/mapping/converted/enums/Person.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( Environment.PREFER_NATIVE_ENUM_TYPES, "false" );
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
	@TestForIssue(jiraKey = "HHH-8153")
	public void hbmEnumTypeTest() {
		doInHibernate(
				this::sessionFactory,
				s -> {
					assertEquals( getNumberOfPersonByGender( s, Gender.MALE ), 2 );
					assertEquals( getNumberOfPersonByGenderAndHairColor( s, Gender.MALE, HairColor.BROWN ), 1 );
					assertEquals( getNumberOfPersonByGender( s, Gender.FEMALE ), 2 );
					assertEquals( getNumberOfPersonByGenderAndHairColor( s, Gender.FEMALE, HairColor.BROWN ), 1 );
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
}
