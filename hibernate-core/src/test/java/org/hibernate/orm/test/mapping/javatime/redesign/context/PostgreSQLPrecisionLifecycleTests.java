package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.cache.MapStorageAccessImpl;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// PostgreSQL mapping-local precision across assignments, database reads and real
/// cache entries. Expected values are explicit independent live-evidence vectors.
/// Both bootstrap modes retain every carrier, including the adjusted clock.
///
/// @author Steve Ebersole
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
class PostgreSQLPrecisionLifecycleTests {
	private static final OffsetDateTime STAMP = OffsetDateTime.parse( "1999-12-31T23:59:59.123500Z" );
	private static final OffsetTime CLOCK = OffsetTime.parse( "23:59:59.123499500+05:45:30" );

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void writeCacheDatabaseCacheAndDirtyChecking(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.type.temporal_preservation", true, "hibernate.cache.region.factory_class", SerializingCache.class ), Event.class ) ) {
			final var event = new Event();
			event.id = 1;
			event.nativeStamp = STAMP;
			event.micros = STAMP.withNano( 123_499_999 );
			event.automatic = CLOCK;
			event.seconds = OffsetTime.parse( "23:59:59.900+05:45:30" );
			event.adjustedStamp = STAMP;
			event.nativeClock = CLOCK;
			event.adjustedClock = OffsetTime.parse( "18:14:29.123499500Z" );
			scope.tx( session -> session.persist( event ) );
			assertThat( event.nativeStamp ).isEqualTo( STAMP );
			assertThat( event.nativeClock ).isEqualTo( CLOCK );
			scope.measure();
			scope.tx( session -> check( session.find( Event.class, 1 ) ) );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> check( session.find( Event.class, 1 ) ) );
			scope.tx( session -> check( session.find( Event.class, 1 ) ) );
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 2 );

			scope.measure();
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.nativeStamp = loaded.nativeStamp.withNano( 123_100_000 );
				loaded.nativeClock = loaded.nativeClock.withNano( 123_100_000 );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isZero();
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.nativeStamp = loaded.nativeStamp.withOffsetSameInstant( ZoneOffset.ofHours( 2 ) );
			} );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.nativeClock = null;
				loaded.adjustedStamp = null;
			} );
			scope.factory.getCache().evictAllRegions();
			scope.tx( session -> {
				final var loaded = session.find( Event.class, 1 );
				assertThat( loaded.nativeClock ).isNull();
				assertThat( loaded.adjustedStamp ).isNull();
			} );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void convertedInstantIsNotRecreatedForCache(boolean metadata) {
		try ( var scope = ContextScope.open( metadata, Map.of( "hibernate.type.temporal_preservation", true, "hibernate.cache.region.factory_class", SerializingCache.class ), Converted.class ) ) {
			final var row = new Converted(); row.id = 1; row.stamp = "2026-01-01T00:00:00.123499999Z";
			TextInstant.writes.set( 0 );
			scope.tx( session -> session.persist( row ) );
			assertThat( TextInstant.writes ).hasValue( 1 );
			assertThat( row.stamp ).isEqualTo( "2026-01-01T00:00:00.123499999Z" );
			scope.measure();
			for ( int pass = 0; pass < 3; pass++ ) {
				if ( pass == 1 ) { scope.factory.getCache().evictAllRegions(); }
				scope.tx( session -> assertThat( session.find( Converted.class, 1 ).stamp ).isEqualTo( "2026-01-01T00:00:00.124Z" ) );
				assertThat( TextInstant.writes ).hasValue( 1 );
			}
			assertThat( scope.factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 2 );
			scope.tx( session -> session.find( Converted.class, 1 ).stamp = "2026-01-01T00:00:00.124100Z" );
			assertThat( TextInstant.writes ).hasValue( 2 );
			assertThat( scope.factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
		}
	}

	@Entity(name = "PgConvertedPrecision")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Converted {
		@Id int id;
		@Convert(converter = TextInstant.class) @Column(secondPrecision = 3) String stamp;
	}

	/// Counts application conversion independently of cache reads and dirty checking.
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

	private static void check(Event event) {
		assertThat( event.micros ).isEqualTo( OffsetDateTime.parse( "1999-12-31T23:59:59.123500Z" ) );
		assertThat( event.automatic ).isEqualTo( OffsetTime.parse( "23:59:59+05:45:30" ) );
		assertThat( event.seconds ).isEqualTo( OffsetTime.parse( "00:00+05:45:30" ) );
		assertThat( event.nativeStamp ).isEqualTo( OffsetDateTime.parse( "1999-12-31T23:59:59.123Z" ) );
		assertThat( event.adjustedStamp ).isEqualTo( OffsetDateTime.parse( "2000-01-01T05:29:59.123+05:30" ) );
		assertThat( event.nativeClock ).isEqualTo( OffsetTime.parse( "23:59:59.123+05:45:30" ) );
		assertThat( event.adjustedClock ).isEqualTo( OffsetTime.parse( "12:14:29.124+18:00" ) );
	}

	@Entity(name = "PgPrecisionEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		@Column(secondPrecision = 3)
		@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
		OffsetDateTime nativeStamp;
		@Column(secondPrecision = 6) @TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED) OffsetDateTime micros;
		OffsetTime automatic;
		@Column(secondPrecision = 0) OffsetTime seconds;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+05:30") OffsetDateTime adjustedStamp;
		@Column(secondPrecision = 3) OffsetTime nativeClock;
		@Column(secondPrecision = 3) @TimeZoneAdjustment("+18:00") OffsetTime adjustedClock;
	}
	/// Real entity cache entries cross serialization boundaries on both publication and read.
	///
	/// @author Steve Ebersole
	public static class SerializingCache extends CachingRegionFactory {
		@Override
		public DomainDataRegion buildDomainDataRegion(DomainDataRegionConfig config, DomainDataRegionBuildingContext context) {
			return new DomainDataRegionImpl( config, this, new MapStorageAccessImpl() {
				@Override public Object getFromCache(Object key, SharedSessionContractImplementor session) {
					return copy( super.getFromCache( key, session ) );
				}
				@Override public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
					super.putIntoCache( key, copy( value ), session );
				}
			}, DefaultCacheKeysFactory.INSTANCE, context );
		}
		private static Object copy(Object value) {
			if ( value == null ) { return null; }
			try {
				final var bytes = new ByteArrayOutputStream();
				try ( var output = new ObjectOutputStream( bytes ) ) { output.writeObject( value ); }
				try ( var input = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) { return input.readObject(); }
			}
			catch (Exception failure) { throw new AssertionError( "Temporal entity cache serialization", failure ); }
		}
	}

}
