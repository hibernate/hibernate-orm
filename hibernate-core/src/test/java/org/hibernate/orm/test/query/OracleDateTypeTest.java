/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SessionFactory
@DomainModel(annotatedClasses = {
		OracleDateTypeTest.AuthorDate.class,
		OracleDateTypeTest.AuthorTimestamp.class,
		OracleDateTypeTest.AuthorTimestampTz.class
})
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-18837")
public class OracleDateTypeTest {

	@Test
	void hhh18837test_withDate(SessionFactoryScope scope) {

		// given
		LocalDate birth = LocalDate.of( 2013, 7, 5 );

		// prepare
		scope.inTransaction( session -> {

			AuthorDate authorDate = new AuthorDate();
			authorDate.birth = birth;
			session.persist( authorDate );
		} );

		// assert
		scope.inTransaction( session -> {
			Long epoch = session.createSelectionQuery( "select epoch(a.birth) from AuthorDate a", Long.class )
					.getSingleResult();
			assertEquals( birth.atStartOfDay( ZoneOffset.UTC ).toInstant().toEpochMilli() / 1000, epoch );
		} );
	}

	@Test
	void hhh18837test_withTimestamp(SessionFactoryScope scope) {

		// given
		LocalDateTime birth = LocalDate.of( 2013, 7, 5 ).atTime( LocalTime.MIN );

		// prepare
		scope.inTransaction( session -> {

			AuthorTimestamp authorTimestamp = new AuthorTimestamp();
			authorTimestamp.birth = birth;
			session.persist( authorTimestamp );
		} );

		// assert
		scope.inTransaction( session -> {
			Long epoch = session.createSelectionQuery( "select epoch(a.birth) from AuthorTimestamp a", Long.class )
					.getSingleResult();
			assertEquals( birth.toEpochSecond( ZoneOffset.UTC ), epoch );
		} );
	}

	@Test
	void hhh18837test_withTimestampTz(SessionFactoryScope scope) {

		// given
		ZonedDateTime birth = ZonedDateTime.of( LocalDate.of( 2013, 7, 5 ), LocalTime.MIN,
				ZoneId.of( "Europe/Vienna" ) );

		// prepare
		scope.inTransaction( session -> {

			AuthorTimestampTz authorTimestampTz = new AuthorTimestampTz();
			authorTimestampTz.birth = birth;
			session.persist( authorTimestampTz );
		} );

		// assert
		scope.inTransaction( session -> {
			Long epoch = session.createSelectionQuery( "select epoch(a.birth) from AuthorTimestampTz a", Long.class )
					.getSingleResult();
			assertEquals( birth.toEpochSecond(), epoch );
		} );
	}

	@AfterAll
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "AuthorDate")
	public static class AuthorDate {

		@Id
		@GeneratedValue
		long id;

		LocalDate birth;
	}

	@Entity(name = "AuthorTimestamp")
	public static class AuthorTimestamp {

		@Id
		@GeneratedValue
		long id;

		LocalDateTime birth;
	}

	@Entity(name = "AuthorTimestampTz")
	public static class AuthorTimestampTz {

		@Id
		@GeneratedValue
		long id;

		ZonedDateTime birth;
	}
}
