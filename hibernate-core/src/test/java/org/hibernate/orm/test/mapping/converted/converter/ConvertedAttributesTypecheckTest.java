/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.time.Duration;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = ConvertedAttributesTypecheckTest.TestEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17693" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17766" )
public class ConvertedAttributesTypecheckTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity(
				Set.of( "one", "two" ),
				"123",
				"3"
		) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Test
	public void testLikeOnConvertedString(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where convertedString like 'one%'",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getConvertedString() ).contains( "one" );
		} );
	}

	@Test
	public void testBinaryArithmeticOnConvertedNumber(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select convertedNumber - 123 from TestEntity",
					Integer.class
			).getSingleResult() ).isEqualTo( 0 );
			assertThat( session.createQuery(
					"select 123 + convertedNumber from TestEntity",
					Integer.class
			).getSingleResult() ).isEqualTo( 246 );
		} );
	}

	@Test
	public void testUnaryExpressionOnConvertedNumber(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select -convertedNumber from TestEntity",
					Integer.class
			).getSingleResult() ).isEqualTo( -123 );
			assertThat( session.createQuery(
					"from TestEntity where -convertedNumber = -123",
					TestEntity.class
			).getSingleResult().getConvertedNumber() ).isEqualTo( "123" );
		} );
	}

	@Test
	public void testFromDurationExpressionOnConvertedDuration(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select convertedDuration by day from TestEntity",
					Long.class
			).getSingleResult() ).isEqualTo( 3L );
			assertThat( session.createQuery(
					"from TestEntity where convertedDuration by day = 3",
					TestEntity.class
			).getSingleResult().getConvertedDuration() ).isEqualTo( "3" );
		} );
	}

	@Entity( name = "TestEntity" )
	public static class TestEntity {
		@Id
		@GeneratedValue
		public Long id;

		@Convert( converter = SetConverter.class )
		public Set<String> convertedString;

		@Convert( converter = StringConverter.class )
		public String convertedNumber;

		@Convert( converter = DurationConverter.class )
		public String convertedDuration;

		public TestEntity() {
		}

		public TestEntity(Set<String> convertedString, String convertedNumber, String convertedDuration) {
			this.convertedString = convertedString;
			this.convertedNumber = convertedNumber;
			this.convertedDuration = convertedDuration;
		}

		public Set<String> getConvertedString() {
			return convertedString;
		}

		public String getConvertedNumber() {
			return convertedNumber;
		}

		public String getConvertedDuration() {
			return convertedDuration;
		}
	}

	@Converter
	public static class SetConverter implements AttributeConverter<Set<String>, String> {
		@Override
		public String convertToDatabaseColumn(final Set<String> attribute) {
			return attribute == null ? null : String.join( ",", attribute );
		}

		@Override
		public Set<String> convertToEntityAttribute(final String dbData) {
			return dbData == null ? null : Set.of( dbData.split( "," ) );
		}
	}

	@Converter
	public static class StringConverter implements AttributeConverter<String, Integer> {
		@Override
		public Integer convertToDatabaseColumn(final String attribute) {
			return attribute == null ? null : Integer.valueOf( attribute );
		}

		@Override
		public String convertToEntityAttribute(final Integer dbData) {
			return dbData == null ? null : dbData.toString();
		}
	}

	@Converter
	public static class DurationConverter implements AttributeConverter<String, Duration> {
		@Override
		public Duration convertToDatabaseColumn(final String attribute) {
			return attribute == null ? null : Duration.ofDays( Long.parseLong( attribute ) );
		}

		@Override
		public String convertToEntityAttribute(final Duration dbData) {
			return dbData == null ? null : String.valueOf( dbData.toDays() );
		}
	}
}
