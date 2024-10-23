/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.hibernate.annotations.JavaType;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for @see org.hibernate.type.descriptor.java.JdbcTimeJavaType checking that retrieved value of
 *
 * @see java.sql.Time field is equal to saved one.
 * <p>
 * Date/time used for testing is June 20th 2024 at midnight.
 * <p>
 * Three identical tests are created, for three different time zones:
 * - good : America/Chihuahua - time zone offset at test time is identical to offset at January 1st 1970; this test is successfull for all databases
 * - bad :  America/Cancun - time zone offsets at test time is different from offset at January 1st 1970; this test is failing for some databases
 * - ugly : America/Hermosillo - there is gap at midnight of January 1st 1970, so that year 1970 starts at 01:00 instead at 00:00; all tests with local time where local time is zero will fail
 *
 * [IANA Time Zone Database version 2024b (2024-09-04) removed 'uglay' time zones]
 */

@DomainModel(annotatedClasses = GoodBadUglyTest.Times.class)
@SessionFactory
@SkipForDialect(dialectClass = InformixDialect.class)
public class GoodBadUglyTest {

	@BeforeEach
	void clearData(final SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SqlTimeTest" ).executeUpdate() );
	}

	@Test
	void good(final SessionFactoryScope scope) {
		test( scope, "America/Chihuahua" );
	}

	@Test
	void bad(final SessionFactoryScope scope) {
		test( scope, "America/Cancun" );
	}

	@Test
	@Disabled
	void ugly(final SessionFactoryScope scope) {
		test( scope, "America/Hermosillo" );
	}

	private static void test(final SessionFactoryScope scope, final String zoneId) {
		final var original = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( zoneId ) );
		try {
			final var now = LocalDate.of( 2024, 6, 20 ).atStartOfDay( ZoneId.systemDefault() );
			final var expected = scope.fromTransaction( session -> {
				final var entity = new Times( 1 );
				entity.sqlTime = new Time( now.toInstant().toEpochMilli() );
				session.persist( entity );
				return entity;
			} );

			final var actual = scope.fromSession( session -> session.get( Times.class, expected.id ) );

			assertTrue(
					JdbcTimeJavaType.INSTANCE.areEqual( expected.sqlTime, actual.sqlTime ),
					"expected %s, but was %s".formatted( expected.sqlTime, actual.sqlTime )
			);

			assertEquals(
					LocalTime.MIDNIGHT,
					actual.sqlTime.toLocalTime(),
					"Expecting that LocalTime.MIDNIGHT is 00:00"
			);
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	@Entity(name = "SqlTimeTest")
	@Table(name = "sql_time_test")
	public static class Times {
		@Id
		private Integer id;

		@JavaType(JdbcTimeJavaType.class)
		private Time sqlTime;


		public Times() {
		}

		public Times(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public Time getSqlTime() {
			return sqlTime;
		}

		public void setSqlTime(final Time sqlTime) {
			this.sqlTime = sqlTime;
		}

		@Override
		public String toString() {
			return "Times{id=%d, sqlTime=%s}".formatted( id, sqlTime );
		}
	}
}
