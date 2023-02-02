/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.mutabiity;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.hibernate.annotations.Immutable;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16081" )
@DomainModel( annotatedClasses = ConvertedMutabilityTests.TestEntityWithDates.class )
@SessionFactory( useCollectingStatementInspector = true )
public class ConvertedMutabilityTests {
	private static final Instant START = Instant.now();

	@Test
	void testImmutableDate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final TestEntityWithDates loaded = session.get( TestEntityWithDates.class, 1 );

			statementInspector.clear();

			// change `d2` - because it is immutable, this should not trigger an update
			loaded.d2.setTime( Instant.EPOCH.toEpochMilli() );
		} );

		assertThat( statementInspector.getSqlQueries() ).isEmpty();

		scope.inTransaction( (session) -> {
			final TestEntityWithDates loaded = session.get( TestEntityWithDates.class, 1 );
			assertThat( loaded.d1.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	@Test
	void testMutableDate(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final TestEntityWithDates loaded = session.get( TestEntityWithDates.class, 1 );

			statementInspector.clear();

			// change `d1` - because it is mutable, this should trigger an update
			loaded.d1.setTime( Instant.EPOCH.toEpochMilli() );
		} );

		assertThat( statementInspector.getSqlQueries() ).isNotEmpty();

		scope.inTransaction( (session) -> {
			final TestEntityWithDates loaded = session.get( TestEntityWithDates.class, 1 );
			assertThat( loaded.d1.getTime() ).isEqualTo( Instant.EPOCH.toEpochMilli() );
		} );
	}

	@Test
	void testDatesWithMerge(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		final TestEntityWithDates loaded = scope.fromTransaction( (session) -> session.get( TestEntityWithDates.class, 1 ) );

		loaded.d1.setTime( Instant.EPOCH.toEpochMilli() );

		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );
		assertThat( statementInspector.getSqlQueries() ).isNotEmpty();

		final TestEntityWithDates loaded2 = scope.fromTransaction( (session) -> session.get( TestEntityWithDates.class, 1 ) );
		assertThat( loaded2.d1.getTime() ).isEqualTo( Instant.EPOCH.toEpochMilli() );

		loaded2.d2.setTime( Instant.EPOCH.toEpochMilli() );
		statementInspector.clear();
		scope.inTransaction( (session) -> session.merge( loaded ) );
		assertThat( statementInspector.getSqlQueries() ).isNotEmpty();

		final TestEntityWithDates loaded3 = scope.fromTransaction( (session) -> session.get( TestEntityWithDates.class, 1 ) );
		assertThat( loaded3.d2.getTime() ).isEqualTo( START.toEpochMilli() );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TestEntityWithDates(
					1,
					Date.from( START ),
					Date.from( START )
			) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete TestEntityWithDates" ).executeUpdate();
		} );
	}

	public static class DateConverter implements AttributeConverter<Date,String> {
		@Override
		public String convertToDatabaseColumn(Date date) {
			if ( date == null ) {
				return null;
			}
			return DateTimeFormatter.ISO_INSTANT.format( date.toInstant() );
		}

		@Override
		public Date convertToEntityAttribute(String date) {
			if ( StringHelper.isEmpty( date ) ) {
				return null;
			}
			return Date.from( Instant.from( DateTimeFormatter.ISO_INSTANT.parse( date ) ) );
		}
	}

	@Immutable
	public static class ImmutableDateConverter implements AttributeConverter<Date,String> {
		@Override
		public String convertToDatabaseColumn(Date date) {
			if ( date == null ) {
				return null;
			}
			return DateTimeFormatter.ISO_INSTANT.format( date.toInstant() );
		}

		@Override
		public Date convertToEntityAttribute(String date) {
			if ( StringHelper.isEmpty( date ) ) {
				return null;
			}
			return Date.from( Instant.from( DateTimeFormatter.ISO_INSTANT.parse( date ) ) );
		}
	}


	@Entity( name = "TestEntityWithDates" )
	@Table( name = "entity_dates" )
	public static class TestEntityWithDates {
	    @Id
	    private Integer id;

		@Convert( converter = DateConverter.class )
		private Date d1;
		@Convert( converter = ImmutableDateConverter.class )
		private Date d2;

		private TestEntityWithDates() {
			// for use by Hibernate
		}

		public TestEntityWithDates(
				Integer id,
				Date d1,
				Date d2) {
			this.id = id;
			this.d1 = d1;
			this.d2 = d2;
		}
	}

}
