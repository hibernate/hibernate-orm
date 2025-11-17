/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl"),
				@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
		}
)
@DomainModel(
		annotatedClasses = {
				Flight.class,
				Company.class,
				Sky.class
		}
)
@SessionFactory
public class EntityTest {
	private DateFormat df = SimpleDateFormat.getDateTimeInstance( DateFormat.LONG, DateFormat.LONG );

	@Test
	public void testLoad(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) throws Exception {
		//put an object in DB
		assertEquals( "Flight", domainModelScope.getDomainModel().getEntityBinding( Flight.class.getName() ).getTable().getName() );

		sessionFactoryScope.inTransaction(
				session -> {
					Flight firstOne = new Flight();
					firstOne.setId( Long.valueOf( 1 ) );
					firstOne.setName( "AF3202" );
					firstOne.setDuration( new Long( 1000000 ) );
					firstOne.setDurationInSec( 2000 );
					session.persist( firstOne );
					session.flush();
				}
		);

		//read it
		sessionFactoryScope.inTransaction(
				session -> {
					Flight firstOne = session.get( Flight.class, Long.valueOf( 1 ) );
					assertNotNull( firstOne );
					assertEquals( Long.valueOf( 1 ), firstOne.getId() );
					assertEquals( "AF3202", firstOne.getName() );
					assertEquals( Long.valueOf( 1000000 ), firstOne.getDuration() );
					assertFalse( 2000l == firstOne.getDurationInSec(), "Transient is not working" );
				}
		);
	}

	@Test
	public void testColumn(SessionFactoryScope scope) {
		//put an object in DB
		scope.inTransaction(
				session -> {
					Flight firstOne = new Flight();
					firstOne.setId( Long.valueOf( 1 ) );
					firstOne.setName( "AF3202" );
					firstOne.setDuration( Long.valueOf( 1000000 ) );
					firstOne.setDurationInSec( 2000 );
					session.persist( firstOne );
					session.flush();
				}
		);

		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					Flight firstOne = new Flight();
					firstOne.setId( Long.valueOf( 1 ) );
					firstOne.setName( null );

					try {
						session.persist( firstOne );
						tx.commit();
						fail( "Name column should be not null" );
					}
					catch (HibernateException e) {
						//fine
					}
				}
		);

		//insert an object and check that name is not updatable
		scope.inTransaction(
				session -> {
					Flight firstOne = new Flight();
					firstOne.setId( Long.valueOf( 1 ) );
					firstOne.setName( "AF3202" );
					firstOne.setTriggeredData( "should not be insertable" );
				}
		);

		scope.inTransaction(
				session -> {
					Flight firstOne = session.get( Flight.class, Long.valueOf( 1 ) );
					assertNotNull( firstOne );
					assertEquals( Long.valueOf( 1 ), firstOne.getId() );
					assertEquals( "AF3202", firstOne.getName() );
					assertFalse( "should not be insertable".equals( firstOne.getTriggeredData() ) );
					firstOne.setName( "BA1234" );
					firstOne.setTriggeredData( "should not be updatable" );
				}
		);

		scope.inTransaction(
				session -> {
					Flight firstOne = session.get( Flight.class, Long.valueOf( 1 ) );
					assertNotNull( firstOne );
					assertEquals( Long.valueOf( 1 ), firstOne.getId() );
					assertEquals( "AF3202", firstOne.getName() );
					assertFalse( "should not be updatable".equals( firstOne.getTriggeredData() ) );
				}
		);
	}

	@Test
	public void testColumnUnique(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					Sky sky = new Sky();
					sky.id = Long.valueOf( 2 );
					sky.color = "blue";
					sky.day = "monday";
					sky.month = "January";

					Sky sameSky = new Sky();
					sameSky.id = Long.valueOf( 3 );
					sameSky.color = "blue";
					sky.day = "tuesday";
					sky.month = "January";

					try {
						session.persist( sky );
						session.flush();
						session.persist( sameSky );
						tx.commit();
						fail( "unique constraints not respected" );
					}
					catch (HibernateException e) {
						//success
					}
					finally {
						if ( tx != null ) {
							tx.rollback();
						}
					}
				}
		);
	}

	@Test
	public void testUniqueConstraint(SessionFactoryScope scope) {
		int id = 5;
		Sky sky = new Sky();
		sky.id = Long.valueOf( id++ );
		sky.color = "green";
		sky.day = "monday";
		sky.month = "March";

		Sky otherSky = new Sky();
		otherSky.id = Long.valueOf( id++ );
		otherSky.color = "red";
		otherSky.day = "friday";
		otherSky.month = "March";

		Sky sameSky = new Sky();
		sameSky.id = Long.valueOf( id++ );
		sameSky.color = "green";
		sameSky.day = "monday";
		sameSky.month = "March";

		scope.inTransaction(
				session -> {

					session.persist( sky );
					session.flush();

					session.persist( otherSky );
				}
		);

		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						session.persist( sameSky );
						tx.commit();
						fail( "unique constraints not respected" );
					}
					catch (PersistenceException e) {
						//success
						if ( tx != null ) {
							tx.rollback();
						}
					}
				}
		);
	}

	@Test
	public void testVersion(SessionFactoryScope scope) {
//		put an object in DB
		scope.inTransaction(
				session -> {
					Flight firstOne = new Flight();
					firstOne.setId( Long.valueOf( 2 ) );
					firstOne.setName( "AF3202" );
					firstOne.setDuration( Long.valueOf( 500 ) );
					session.persist( firstOne );
					session.flush();
				}
		);

		//read it
		Flight firstOne = scope.fromTransaction(
				session -> session.get( Flight.class, Long.valueOf( 2 ) )
		);

		//read it again
		Flight concurrentOne = scope.fromTransaction(
				session -> {
					Flight _concurrentOne = session.get( Flight.class, Long.valueOf( 2 ) );
					_concurrentOne.setDuration( Long.valueOf( 1000 ) );
					return session.merge( _concurrentOne );
				}
		);

		assertFalse( firstOne == concurrentOne );
		assertFalse( firstOne.getVersion().equals( concurrentOne.getVersion() ) );

		//reattach the first one
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					firstOne.setName( "Second access" );
					try {
						session.merge( firstOne );
						tx.commit();
						fail( "Optimistic locking should work" );
					}
					catch (OptimisticLockException expected) {
						if ( expected.getCause() instanceof StaleStateException ) {
							//expected
						}
						else {
							fail( "StaleStateException expected but is " + expected.getCause() );
						}
					}
					finally {
						if ( tx != null ) {
							tx.rollback();
						}
					}
				}
		);
	}

	@Test
	public void testFieldAccess(SessionFactoryScope scope) {
		final Sky sky = new Sky();
		sky.id = Long.valueOf( 1 );
		sky.color = "black";
		sky.area = "Paris";
		sky.day = "23";
		sky.month = "1";

		scope.inTransaction(
				session -> session.persist( sky )
		);

		sky.area = "London";

		scope.inTransaction(
				session -> {
					Sky _sky = session.get( Sky.class, sky.id );
					assertNotNull( _sky );
					assertEquals( "black", _sky.color );
					assertFalse( "Paris".equals( _sky.area ) );
				}
		);
	}

	@Test
	public void testEntityName(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		assertEquals( "Corporation", domainModelScope.getDomainModel().getEntityBinding( Company.class.getName() ).getTable().getName() );

		sessionFactoryScope.inTransaction(
				session -> {
					Company comp = new Company();
					session.persist( comp );
					comp.setName( "JBoss Inc" );
				}
		);

		sessionFactoryScope.inTransaction(
				session -> {
					List result = session.createQuery( "from Corporation" ).list();
					assertNotNull( result );
					assertEquals( 1, result.size() );
				}
		);

	}

	@Test
	public void testNonGetter(SessionFactoryScope scope) {
		Flight airFrance = new Flight();
		airFrance.setId( Long.valueOf( 747 ) );
		airFrance.setName( "Paris-Amsterdam" );
		airFrance.setDuration( Long.valueOf( 10 ) );
		airFrance.setFactor( 25 );

		scope.inTransaction(
				session -> session.persist( airFrance )
		);

		scope.inTransaction(
				session -> {
					Flight _airFrance = session.get( Flight.class, airFrance.getId() );
					assertNotNull( _airFrance );
					assertEquals( Long.valueOf( 10 ), _airFrance.getDuration() );
					assertFalse( 25 == _airFrance.getFactor( false ) );
					session.remove( _airFrance );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 10, reason = "oracle12c returns time in getDate.  For now, skip.")
	public void testTemporalType(SessionFactoryScope scope) {
		final ZoneId zoneId = ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) ? ZoneId.of( "UTC")
				: ZoneId.systemDefault();

		Flight airFrance = new Flight();
		airFrance.setId( Long.valueOf( 747 ) );
		airFrance.setName( "Paris-Amsterdam" );
		airFrance.setDuration( Long.valueOf( 10 ) );
		airFrance.setDepartureDate( Date.from(LocalDate.of( 2005, 06, 21 ).atStartOfDay(zoneId).toInstant()) );
		airFrance.setAlternativeDepartureDate( new GregorianCalendar( 2006, 02, 03, 10, 00 ) );
		airFrance.getAlternativeDepartureDate().setTimeZone( TimeZone.getTimeZone( "GMT" ) );
		airFrance.setBuyDate( new java.sql.Timestamp( 122367443 ) );
		airFrance.setFactor( 25 );

		scope.inTransaction(
				session -> session.persist( airFrance )
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from Flight f where f.departureDate = :departureDate" );
					q.setParameter( "departureDate", airFrance.getDepartureDate(), StandardBasicTypes.DATE );
					Flight copyAirFrance = (Flight) q.uniqueResult();
					assertNotNull( copyAirFrance );
					assertEquals(
							Date.from(LocalDate.of( 2005, 06, 21 ).atStartOfDay(zoneId).toInstant()),
							copyAirFrance.getDepartureDate()
					);
					assertEquals( df.format( airFrance.getBuyDate() ), df.format( copyAirFrance.getBuyDate() ) );

					session.remove( copyAirFrance );
				}
		);
	}

	@Test
	public void testBasic(SessionFactoryScope scope) throws Exception {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					Flight airFrance = new Flight();
					airFrance.setId( Long.valueOf( 747 ) );
					airFrance.setName( "Paris-Amsterdam" );
					airFrance.setDuration( null );
					try {
						session.persist( airFrance );
						tx.commit();
						fail( "Basic(optional=false) fails" );
					}
					catch (Exception e) {
						//success
						if ( tx != null ) {
							tx.rollback();
						}
					}
				}
		);
	}

	@AfterEach
	public void runDropSchema(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
