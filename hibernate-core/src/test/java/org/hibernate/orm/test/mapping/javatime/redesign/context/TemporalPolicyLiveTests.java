package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.internal.TemporalMappingValidator;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.annotations.TemporalPreservationPolicy.Type.NORMALIZED;

/// Real JDBC round trips establish guarantees independently of the TCK's ID comparisons.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalPolicyLiveTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void normalizedRoundTripCacheAndPredicates(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of(
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_DATETIME, NORMALIZED,
				MappingSettings.TEMPORAL_PRESERVATION_POLICY_OFFSET_TIME, NORMALIZED,
				"hibernate.jdbc.time_zone", "America/Los_Angeles" ), Event.class ) ) {
			int id = 0;
			for ( String text : List.of( "00:30:00+02:00", "23:30:00-02:00", "10:00:00+02:00" ) ) {
				final var row = new Event();
				row.id = ++id;
				row.stamp = OffsetDateTime.parse( "2026-11-01T01:30:00.123456-07:00" );
				row.time = OffsetTime.parse( text );
				row.adjustedTime = row.time;
				scope.tx( session -> { setTimeZone( scope, session, "+05:30" ); session.persist( row ); } );
				scope.tx( session -> assertThat( session.find( Event.class, row.id ).stamp.toInstant() ).isEqualTo( row.stamp.toInstant() ) );
				scope.factory.getCache().evictAllRegions();
				scope.tx( session -> {
					setTimeZone( scope, session, "-04:00" );
					final var actual = session.find( Event.class, row.id );
					assertThat( actual.stamp.toInstant() ).isEqualTo( row.stamp.toInstant() );
					assertThat( actual.adjustedTime ).isEqualTo( row.time.withOffsetSameInstant( ZoneOffset.ofHoursMinutes( 5, 30 ) ) );
					assertThat( actual.time.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() )
							.isEqualTo( row.time.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() );
					assertThat( session.createQuery( "select id from PolicyLiveEvent where id=:id and stamp=:stamp", Integer.class )
							.setParameter( "id", row.id ).setParameter( "stamp", row.stamp.withOffsetSameInstant( ZoneOffset.UTC ) )
							.getSingleResult() ).isEqualTo( row.id );
				} );
			}
			scope.measure();
			scope.tx( session -> session.find( Event.class, 1 ) );
			scope.noMutation();
			scope.measure();
			scope.tx( session -> {
				final var row = session.find( Event.class, 1 );
				row.time = row.time.withOffsetSameInstant( row.time.getOffset().equals( ZoneOffset.ofHours( 4 ) )
						? ZoneOffset.ofHours( -3 ) : ZoneOffset.ofHours( 4 ) );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@RequiresDialectFeature(feature = TemporalEligibilityChecks.NormalizedOffsetDateTimeProducer.class)
	void generatedVersionStillEnforcesOptimisticLocking(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Versioned.class ) ) {
			final var mapping = (BasicValue) scope.metadata.getEntityBinding( Versioned.class.getName() ).getVersion().getValue();
			TemporalMappingValidator.validate( scope.factory.getJdbcServices().getDialect(),
					mapping.resolve().getLegacyResolvedBasicType(), "explicit mapped callable type", Map.of() );
			final var row = new Versioned(); row.id = 1;
			scope.tx( session -> session.persist( row ) );
			assertThat( row.version ).isNotNull();
			try ( var session = scope.factory.openSession() ) {
				final var stale = session.find( Versioned.class, 1 );
				scope.tx( other -> { var current = other.find( Versioned.class, 1 ); current.data = "updated"; } );
				stale.data = "stale";
				final var transaction = session.beginTransaction();
				assertThatThrownBy( session::flush ).isInstanceOf( OptimisticLockException.class );
				transaction.rollback();
			}
		}
	}

	private static void setTimeZone(ContextScope scope, Session session, String offset) {
		final String prefix = scope.factory.getJdbcServices().getDialect() instanceof OracleDialect
				? "alter session set time_zone = '" : "set time zone '";
		session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( prefix + offset + "'" );
			}
		} );
	}

	@Entity(name = "PolicyLiveEvent")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		OffsetDateTime stamp;
		OffsetTime time;
		@TimeZoneAdjustment("+05:30") OffsetTime adjustedTime;
	}

	@Entity(name = "PolicyLiveVersion")
	static class Versioned {
		@Id int id;
		@Version @CurrentTimestamp @TemporalPreservationPolicy(NORMALIZED) OffsetDateTime version;
		String data;
	}
}
