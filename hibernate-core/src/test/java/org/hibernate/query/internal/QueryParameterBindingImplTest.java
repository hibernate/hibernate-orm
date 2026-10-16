package org.hibernate.query.internal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusing ONE named parameter at THREE or more places, spread over at least TWO different attributes that carry the
 * same {@code AttributeConverter}, fails to bind.
 *
 * <pre>
 * java.lang.IllegalArgumentException: Unsure how to handle given Java type [java.time.LocalDateTime]
 *                                     as TemporalType#TIMESTAMP
 * </pre>
 * <p>
 */
@JiraKey(value = "HHH-20955")
@DomainModel(annotatedClasses = Record.class)
@SessionFactory
final class QueryParameterBindingImplTest {

	private static final OffsetDateTime NOW = OffsetDateTime.parse( "2023-05-03T00:00+02:00" );

	private SessionFactoryScope scope;

	@BeforeEach
	void startUp(SessionFactoryScope scope) {
		this.scope = scope;
		scope.inTransaction( session -> session.persist( new Record(
						1L,
						OffsetDateTime.parse( "2023-05-01T00:00+02:00" ),
						OffsetDateTime.parse( "2099-01-01T00:00+01:00" ),
						OffsetDateTime.parse( "2023-05-01T00:00+02:00" ),
						OffsetDateTime.parse( "2099-01-01T00:00+01:00" )
				) )
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@DisplayName("3 uses over 3 converted attributes binds and returns the row")
	@ParameterizedTest
	@ValueSource(strings = {
			"""
				SELECT r FROM Record r WHERE
				r.validFrom <= :now
				AND r.validTo > :now
				AND r.recordedFrom <= :now
				"""
			, """
				SELECT r FROM Record r WHERE
				r.validFrom <= :now
				AND r.validTo > :now
				AND r.recordedFrom <= :now
				AND r.validFrom <= :now
				AND r.validTo > :now
				AND r.recordedFrom <= :now
				""" // This is just repeating the same parameters twice
	})
	void canBindThreeAttributesAgainstSingleNamedParameter(String sql) {
		// Act
		var query = query( sql );

		// Assert
		assertThat( query ).hasSize( 1 );
	}

	private List<Record> query(String whereClause) {
		return query( whereClause, Map.of( "now", NOW ) );
	}

	private List<Record> query(String sql, Map<String, Object> parameterToValue) {
		return scope.fromSession( session -> {
			var query = session.createQuery( sql, Record.class );

			parameterToValue.forEach( query::setParameter );

			return query.getResultList();
		} );
	}

	@DisplayName("All four validity attributes, one parameter")
	@Test
	void canBindFourAttributesAgainstSingleNamedParameter() {
		// Arrange
		var sql = """
				SELECT r FROM Record r WHERE
				r.validFrom <= :now
				AND r.validTo > :now
				AND r.recordedFrom <= :now
				AND r.recordedTo > :now
				""";

		// Act
		var query = query( sql );

		// Assert
		assertThat( query ).hasSize( 1 );
	}

	@DisplayName("Filtering still works, i.e. the value is bound correctly and not merely accepted")
	@Test
	void canStillFilterData() {
		// Arrange
		var sqlQuery = """
				SELECT r FROM Record r WHERE
				r.validFrom <= :now
				AND r.validTo > :now
				AND r.recordedFrom <= :now
				AND r.recordedTo > :now
				""";

		Map<String, Object> parameters = Map.of( "now", OffsetDateTime.parse( "2020-01-01T00:00+01:00" ) );

		// Act
		var query = query( sqlQuery, parameters );

		// Assert
		assertThat( query )
				.as( "a point in time before the row's validity must not match" )
				.isEmpty();
	}
}
