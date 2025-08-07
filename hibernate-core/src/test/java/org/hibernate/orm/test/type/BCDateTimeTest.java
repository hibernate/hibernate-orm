/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.descriptor.java.CalendarDateJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE")
})
@Jira("https://hibernate.atlassian.net/browse/HHH-18589")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDatesInBCEra.class)
@SkipForDialect(dialectClass = HSQLDialect.class, reason = "Bug in HSQLDB https://sourceforge.net/p/hsqldb/bugs/1737/")
public class BCDateTimeTest {

	private static final Instant INSTANT = Instant.parse("-0001-01-01T00:00:00.0Z");

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = new EntityOfBasics();
			entity.setId( 1 );
			// Transform the Instant to java.sql.Timestamp/java.sql.Date/Calendar through our JavaType implementations,
			// since these will account for conversion between Gregorian and Julian epoch
			entity.setTheTimestamp( JdbcTimestampJavaType.INSTANCE.wrap( INSTANT, session ) );
			entity.setTheDate( JdbcDateJavaType.INSTANCE.wrap( INSTANT, session ) );
			entity.setTheTimestampCalendar( CalendarJavaType.INSTANCE.wrap( INSTANT, session ) );
			entity.setTheDateCalendar( CalendarDateJavaType.INSTANCE.wrap( INSTANT, session ) );
			entity.setTheInstant( INSTANT );
			entity.setTheLocalDateTime( LocalDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ) );
			entity.setTheLocalDate( LocalDate.ofInstant( INSTANT, ZoneId.of( "UTC" ) ) );
			entity.setTheOffsetDateTime( OffsetDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ) );
			entity.setTheZonedDateTime( ZonedDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ) );
			session.persist( entity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testRead(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertEquals( JdbcTimestampJavaType.INSTANCE.wrap( INSTANT, session ), entity.getTheTimestamp() );
			assertEquals( JdbcDateJavaType.INSTANCE.wrap( INSTANT, session ), entity.getTheDate() );
			assertEquals( CalendarJavaType.INSTANCE.wrap( INSTANT, session ), entity.getTheTimestampCalendar() );
			assertEquals( CalendarDateJavaType.INSTANCE.wrap( INSTANT, session ), entity.getTheDateCalendar() );
			assertEquals( INSTANT, entity.getTheInstant() );
			assertEquals( LocalDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ), entity.getTheLocalDateTime() );
			assertEquals( LocalDate.ofInstant( INSTANT, ZoneId.of( "UTC" ) ), entity.getTheLocalDate() );
			assertEquals( OffsetDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ), entity.getTheOffsetDateTime() );
			assertEquals( ZonedDateTime.ofInstant( INSTANT, ZoneId.of( "UTC" ) ), entity.getTheZonedDateTime() );
		});
	}

	@Test
	public void testQueryTimestamp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theTimestamp", entity.getTheTimestamp(), true );
			assertExists( session, "theTimestamp", entity.getTheTimestamp(), false );
		});
	}

	@Test
	public void testQueryDate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theDate", entity.getTheDate(), true );
			assertExists( session, "theDate", entity.getTheDate(), false );
		});
	}

	@Test
	public void testQueryCalendarTimestamp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theTimestampCalendar", entity.getTheTimestampCalendar(), true );
			assertExists( session, "theTimestampCalendar", entity.getTheTimestampCalendar(), false );
		});
	}

	@Test
	public void testQueryCalendarDate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theDateCalendar", entity.getTheDateCalendar(), true );
			assertExists( session, "theDateCalendar", entity.getTheDateCalendar(), false );
		});
	}

	@Test
	public void testQueryInstant(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theInstant", entity.getTheInstant(), true );
			assertExists( session, "theInstant", entity.getTheInstant(), false );
		});
	}

	@Test
	public void testQueryLocalDateTime(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theLocalDateTime", entity.getTheLocalDateTime(), true );
			assertExists( session, "theLocalDateTime", entity.getTheLocalDateTime(), false );
		});
	}

	@Test
	public void testQueryLocalDate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theLocalDate", entity.getTheLocalDate(), true );
			assertExists( session, "theLocalDate", entity.getTheLocalDate(), false );
		});
	}

	@Test
	public void testQueryOffsetDateTime(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theOffsetDateTime", entity.getTheOffsetDateTime(), true );
			assertExists( session, "theOffsetDateTime", entity.getTheOffsetDateTime(), false );
		});
	}

	@Test
	public void testQueryZonedDateTime(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );
			assertExists( session, "theZonedDateTime", entity.getTheZonedDateTime(), true );
			assertExists( session, "theZonedDateTime", entity.getTheZonedDateTime(), false );
		});
	}

	private void assertExists(SessionImplementor session, String field, Object value, boolean parameter) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<EntityOfBasics> query = cb.createQuery( EntityOfBasics.class );
		final JpaRoot<EntityOfBasics> root = query.from( EntityOfBasics.class );
		if ( parameter ) {
			query.where( cb.equal( root.get( field ), value ) );
		}
		else {
			query.where( cb.equal( root.get( field ), cb.literal( value ) ) );
		}
		session.createQuery( query ).getSingleResult();
	}
}
