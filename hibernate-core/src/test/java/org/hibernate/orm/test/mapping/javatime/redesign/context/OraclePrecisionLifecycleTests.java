package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Oracle assignment, serialized cache, database and dirty-checking parity for
/// independently authored precision vectors, with guarded metadata-disabled boot.
///
/// @author Steve Ebersole
@RequiresDialect(value = OracleDialect.class, matchSubTypes = false)
class OraclePrecisionLifecycleTests {
	private static final Map<String, Object> SETTINGS = Map.of(
			"hibernate.type.temporal_preservation", true,
			"hibernate.cache.region.factory_class", PostgreSQLPrecisionLifecycleTests.SerializingCache.class );

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void writeCacheDatabaseCacheDirtyCheckingAndRollback(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, SETTINGS, Event.class ) ) {
			final var event = new Event(); event.id = 1;
			event.stamp = OffsetDateTime.parse( "1999-12-31T23:59:59.123499999Z" );
			event.micros = event.stamp;
			event.nanos = event.stamp.toInstant();
			event.clock = OffsetTime.parse( "23:59:59.123500+05:45:30" );
			event.automatic = OffsetTime.parse( "23:59:59.999999999Z" );
			scope.tx( session -> session.persist( event ) );
			assertThat( event.stamp ).isEqualTo( OffsetDateTime.parse( "1999-12-31T23:59:59.123499999Z" ) );
			assertThat( event.clock ).isEqualTo( OffsetTime.parse( "23:59:59.123500+05:45:30" ) );
			scope.measure();
			for ( int pass = 0; pass < 3; pass++ ) {
				if ( pass == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> check( session.find( Event.class, 1 ) ) );
			}
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 2 );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			scope.measure();
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.stamp = loaded.stamp.withNano( 123_100_000 );
				loaded.clock = loaded.clock.withNano( 124_100_000 );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			scope.tx( session -> session.find( Event.class, 1 ).stamp = OffsetDateTime.parse( "2000-01-01T05:29:59.124+05:30" ) );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.stamp = loaded.stamp.withOffsetSameInstant( ZoneOffset.UTC );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 2 );
			try ( var session = scope.factory.openSession() ) {
				final var transaction = session.beginTransaction();
				session.find( Event.class, 1 ).clock = null;
				session.flush(); transaction.rollback();
			}
			scope.tx( session -> assertThat( session.find( Event.class, 1 ).clock ).isEqualTo( OffsetTime.parse( "12:14:29.124+18:00" ) ) );
			scope.tx( session -> session.find( Event.class, 1 ).clock = null );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> assertThat( session.find( Event.class, 1 ).clock ).isNull() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void convertedInstantUsesTheWrittenValueForCache(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, SETTINGS, Converted.class ) ) {
			final var row = new Converted(); row.id = 1; row.stamp = "2026-01-01T00:00:00.123499999Z";
			TextInstant.writes.set( 0 );
			scope.tx( session -> session.persist( row ) );
			assertThat( TextInstant.writes ).hasValue( 1 );
			assertThat( row.stamp ).isEqualTo( "2026-01-01T00:00:00.123499999Z" );
			scope.measure();
			for ( int pass = 0; pass < 3; pass++ ) {
				if ( pass == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> assertThat( session.find( Converted.class, 1 ).stamp ).isEqualTo( "2026-01-01T00:00:00.123Z" ) );
				assertThat( TextInstant.writes ).hasValue( 1 );
			}
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 2 );
			scope.tx( session -> session.find( Converted.class, 1 ).stamp = "2026-01-01T00:00:00.123100Z" );
			assertThat( TextInstant.writes ).hasValue( 2 );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void simultaneousFactoriesKeepTheirOwnGrids(boolean metadata) {
		try ( var coarse = ContextScope.open( metadata, SETTINGS, Event.class );
				var fine = ContextScope.open( metadata, SETTINGS, Fine.class ) ) {
			final var source = OffsetDateTime.parse( "1999-12-31T23:59:59.123499999Z" );
			final var a = new Event(); a.id = 1; a.stamp = source;
			final var b = new Fine(); b.id = 1; b.stamp = source;
			coarse.tx( session -> session.persist( a ) );
			fine.tx( session -> session.persist( b ) );
			for ( int pass = 0; pass < 2; pass++ ) {
				coarse.tx( session -> assertThat( session.find( Event.class, 1 ).stamp ).isEqualTo( OffsetDateTime.parse( "2000-01-01T05:29:59.123+05:30" ) ) );
				fine.tx( session -> assertThat( session.find( Fine.class, 1 ).stamp ).isEqualTo( OffsetDateTime.parse( "2000-01-01T05:29:59.123500+05:30" ) ) );
				coarse.factory.getCache().evictAllRegions(); fine.factory.getCache().evictAllRegions();
			}
		}
	}

	@Entity(name = "OraFinePrecision")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Fine {
		@Id int id;
		@Column(secondPrecision = 6) @TimeZoneAdjustment("+05:30") OffsetDateTime stamp;
	}

	private static void check(Event event) {
		assertThat( event.stamp ).isEqualTo( OffsetDateTime.parse( "2000-01-01T05:29:59.123+05:30" ) );
		assertThat( event.micros ).isEqualTo( OffsetDateTime.parse( "2000-01-01T05:29:59.123500+05:30" ) );
		assertThat( event.nanos ).isEqualTo( Instant.parse( "1999-12-31T23:59:59.123499999Z" ) );
		assertThat( event.clock ).isEqualTo( OffsetTime.parse( "12:14:29.124+18:00" ) );
		assertThat( event.automatic ).isEqualTo( OffsetTime.parse( "18:00+18:00" ) );
	}

	@Entity(name = "OraPrecisionEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+05:30") OffsetDateTime stamp;
		@Column(secondPrecision = 6) @TimeZoneAdjustment("+05:30") OffsetDateTime micros;
		@Column(secondPrecision = 9) Instant nanos;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+18:00") OffsetTime clock;
		@TimeZoneAdjustment("+18:00") OffsetTime automatic;
	}

	@Entity(name = "OraConvertedPrecision")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Converted {
		@Id int id;
		@Convert(converter = TextInstant.class) @Column(secondPrecision = 3) String stamp;
	}

	/// Counts conversion independently of cache reads and domain dirty checking.
	///
	/// @author Steve Ebersole
	@Converter
	@Immutable
	public static class TextInstant implements AttributeConverter<String, Instant> {
		static final AtomicInteger writes = new AtomicInteger();
		@Override public Instant convertToDatabaseColumn(String value) {
			writes.incrementAndGet(); return value == null ? null : Instant.parse( value );
		}
		@Override public String convertToEntityAttribute(Instant value) { return value == null ? null : value.toString(); }
	}
}
