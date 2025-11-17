/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-4179")
public class DynamicInstantiationTemporalTest {
	@BeforeAll
	public void prepareData(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityOfBasics entityOfBasics = new EntityOfBasics( 1 );
			entityOfBasics.setTheLocalDate( LocalDate.EPOCH );
			entityOfBasics.setTheLocalDateTime( LocalDate.EPOCH.atStartOfDay() );
			entityOfBasics.setTheLocalTime( LocalTime.MIN );
			entityOfBasics.setTheDate( Date.valueOf( entityOfBasics.getTheLocalDate() ) );
			entityOfBasics.setTheTimestamp( Timestamp.valueOf( entityOfBasics.getTheLocalDateTime() ) );
			entityOfBasics.setTheTime( Time.valueOf( entityOfBasics.getTheLocalTime() ) );
			session.persist( entityOfBasics );
		} );
	}

	@AfterAll
	public void cleanUpData(final SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testJavaTime(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery(
					"select new " + JavaTimeDto.class.getName() + "(e.theLocalDate, e.theLocalTime, e.theLocalDateTime) from EntityOfBasics e",
					JavaTimeDto.class
			).getSingleResult();
			EntityOfBasics entityOfBasics = session.find( EntityOfBasics.class, 1 );
			assertEquals( entityOfBasics.getTheLocalDate(), result.getTheLocalDate() );
			assertEquals( entityOfBasics.getTheLocalTime(), result.getTheLocalTime() );
			assertEquals( entityOfBasics.getTheLocalDateTime(), result.getTheLocalDateTime() );
		} );
	}

	@Test
	void testJavaSql(final SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var result = session.createQuery(
					"select new " + JavaSqlDto.class.getName() + "(e.theDate, e.theTime, e.theTimestamp) from EntityOfBasics e",
					JavaSqlDto.class
			).getSingleResult();
			EntityOfBasics entityOfBasics = session.find( EntityOfBasics.class, 1 );
			assertEquals( entityOfBasics.getTheDate(), result.getTheDate() );
			assertEquals( entityOfBasics.getTheTime(), result.getTheTime() );
			assertEquals( entityOfBasics.getTheTimestamp(), result.getTheTimestamp() );
		} );
	}

	public static class JavaSqlDto {
		private java.sql.Date theDate;
		private java.sql.Time theTime;
		private java.sql.Timestamp theTimestamp;

		public JavaSqlDto(Date theDate, Time theTime, Timestamp theTimestamp) {
			this.theDate = theDate;
			this.theTime = theTime;
			this.theTimestamp = theTimestamp;
		}

		public Date getTheDate() {
			return theDate;
		}

		public Time getTheTime() {
			return theTime;
		}

		public Timestamp getTheTimestamp() {
			return theTimestamp;
		}
	}

	public static class JavaTimeDto {
		private LocalDate theLocalDate;
		private LocalTime theLocalTime;
		private LocalDateTime theLocalDateTime;

		public JavaTimeDto(LocalDate theLocalDate, LocalTime theLocalTime, LocalDateTime theLocalDateTime) {
			this.theLocalDate = theLocalDate;
			this.theLocalTime = theLocalTime;
			this.theLocalDateTime = theLocalDateTime;
		}

		public LocalDate getTheLocalDate() {
			return theLocalDate;
		}

		public LocalTime getTheLocalTime() {
			return theLocalTime;
		}

		public LocalDateTime getTheLocalDateTime() {
			return theLocalDateTime;
		}
	}
}
