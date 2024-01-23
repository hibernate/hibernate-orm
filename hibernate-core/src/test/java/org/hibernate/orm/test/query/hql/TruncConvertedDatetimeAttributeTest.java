/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.dialect.DerbyDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = TruncConvertedDatetimeAttributeTest.TestEntity.class )
@SessionFactory
@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby doesn't support any form of date truncation" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17666" )
public class TruncConvertedDatetimeAttributeTest {
	private static final Date DATE = new GregorianCalendar( 2017, Calendar.JANUARY, 24 ).getTime();
	private static final Instant INSTANT = ZonedDateTime.of( 2020, 10, 15, 20, 34, 45, 0, ZoneOffset.UTC ).toInstant();

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 1L, DATE.getTime(), INSTANT ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Test
	public void testTruncSelection(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createQuery(
					"select trunc(instantCol, minute) from TestEntity",
					Instant.class
			).getSingleResult() ).isEqualTo( INSTANT.truncatedTo( ChronoUnit.MINUTES ) );
			assertThat( session.createQuery(
					"select trunc(dateCol, month) from TestEntity",
					Long.class
			).getSingleResult() ).isEqualTo( new GregorianCalendar( 2017, Calendar.JANUARY, 1 ).getTime().getTime() );
		} );
	}

	@Test
	public void testTruncComparison(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createQuery(
					"from TestEntity where trunc(instantCol, hour) < current_date",
					TestEntity.class
			).getResultList() ).hasSize( 1 );
			assertThat( session.createQuery(
					"from TestEntity where trunc(dateCol, year) < current_timestamp",
					TestEntity.class
			).getResultList() ).hasSize( 1 );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		private Long id;

		@Convert( converter = DateConverter.class )
		private Long dateCol;

		@Convert( converter = InstantConverter.class )
		private Instant instantCol;

		public TestEntity() {
		}

		public TestEntity(Long id, Long dateCol, Instant instantCol) {
			this.id = id;
			this.dateCol = dateCol;
			this.instantCol = instantCol;
		}
	}

	@Converter
	public static class DateConverter implements AttributeConverter<Long, Date> {
		public Date convertToDatabaseColumn(Long time) {
			return time == null ? null : new Date( time );
		}

		public Long convertToEntityAttribute(Date date) {
			return date == null ? null : date.getTime();
		}
	}

	@Converter
	public static class InstantConverter implements AttributeConverter<Instant, Timestamp> {
		public Timestamp convertToDatabaseColumn(Instant instant) {
			return instant == null ? null : Timestamp.from( instant );
		}

		public Instant convertToEntityAttribute(Timestamp timestamp) {
			return timestamp == null ? null : timestamp.toInstant();
		}
	}
}
