package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
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
import org.hibernate.annotations.Mutability;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.cache.MapStorageAccessImpl;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Real query-cache payloads cross serialization boundaries. Each factory owns
/// its regions; changing a mapping still requires eviction, not payload adoption.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalQueryCacheLifecycleTests {
	private static final LocalDateTime STORED = LocalDateTime.parse( "1969-12-31T23:59:59.123" );

	@ParameterizedTest
	@CsvSource({ "FULL,true,true", "SHALLOW,true,true", "AUTO,true,true", "FULL,false,true", "FULL,true,false" })
	void layoutsMutableValuesAndIndependentFactories(String layout, boolean entityCache, boolean preservation) {
		for ( int factory = 0; factory < 2; factory++ ) {
			try ( var scope = ContextScope.open( false, Map.of(
					"hibernate.type.temporal_preservation", preservation,
					"hibernate.cache.use_query_cache", true,
					"hibernate.cache.use_second_level_cache", entityCache,
					"hibernate.cache.query_cache_layout", layout,
					"hibernate.cache.region.factory_class", SerializingQueries.class ), Row.class ) ) {
				final var row = new Row(); row.id = 1; row.clock = new Clock( STORED );
				scope.tx( session -> session.persist( row ) );
				ClockConverter.writes.set( 0 );
				scope.measure();
				for ( int pass = 0; pass < 3; pass++ ) {
					scope.tx( session -> {
						final var result = session.createSelectionQuery( "select clock from PrecisionQueryClock", Clock.class )
								.setCacheable( true ).getSingleResult();
						assertThat( result.value ).isEqualTo( STORED );
						result.value = STORED.plusDays( 1 );
						final var entity = session.createSelectionQuery( "from PrecisionQueryClock", Row.class )
								.setCacheable( true ).getSingleResult();
						assertThat( entity.clock.value ).isEqualTo( STORED );
					} );
				}
				assertThat( ClockConverter.writes ).hasValue( 0 );
				assertThat( scope.factory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 4 );
				if ( preservation ) {
					final var input = new Clock( STORED.withNano( 123_100_000 ) );
					scope.tx( session -> {
						final var query = session.createSelectionQuery( "select id from PrecisionQueryClock where clock < :value", Integer.class )
								.setCacheable( true );
						assertThat( query.setParameter( "value", input ).getResultList() ).containsExactly( 1 );
						input.value = STORED.withNano( 122_900_000 );
						assertThat( query.setParameter( "value", input ).getResultList() ).isEmpty();
						input.value = STORED.withNano( 123_100_000 );
						assertThat( query.setParameter( "value", input ).getResultList() ).containsExactly( 1 );
					} );
					assertThat( ClockConverter.writes ).hasValue( 3 );
					assertThat( scope.factory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 5 );
				}
			}
		}
	}

	@Test
	void differentPrecisionFactoriesOwnTheirResultsAndBoundaryKeys() {
		for ( boolean micros : new boolean[] { false, true } ) {
			final var stored = micros ? STORED.withNano( 123_457_000 ) : STORED;
			try ( var scope = ContextScope.open( false, Map.of(
					"hibernate.type.temporal_preservation", true,
					"hibernate.cache.use_query_cache", true,
					"hibernate.cache.region.factory_class", SerializingQueries.class ), micros ? MicroRow.class : Row.class ) ) {
				scope.tx( session -> {
					if ( micros ) {
						final var row = new MicroRow(); row.id = 1; row.clock = new Clock( stored ); session.persist( row );
					}
					else {
						final var row = new Row(); row.id = 1; row.clock = new Clock( stored ); session.persist( row );
					}
				} );
				scope.measure();
				ClockConverter.writes.set( 0 );
				for ( int pass = 0; pass < 2; pass++ ) {
					scope.tx( session -> {
						assertThat( session.createSelectionQuery( "select clock from PrecisionQueryClock", Clock.class )
								.setCacheable( true ).getSingleResult().value ).isEqualTo( stored );
						final var query = session.createSelectionQuery( "select id from PrecisionQueryClock where clock < :value", Integer.class )
								.setCacheable( true );
						assertThat( query.setParameter( "value", new Clock( stored.minusNanos( 100 ) ) ).getResultList() ).isEmpty();
						assertThat( query.setParameter( "value", new Clock( stored.plusNanos( 100 ) ) ).getResultList() ).containsExactly( 1 );
					} );
				}
				assertThat( ClockConverter.writes ).hasValue( 4 );
				assertThat( scope.factory.getStatistics().getQueryCacheMissCount() ).isEqualTo( 3 );
				assertThat( scope.factory.getStatistics().getQueryCacheHitCount() ).isEqualTo( 3 );
			}
		}
	}

	@Entity(name = "PrecisionQueryClock")
	static class MicroRow {
		@Id int id;
		@Convert(converter = ClockConverter.class)
		@Mutability(ClockMutability.class)
		@Column(secondPrecision = 6)
		Clock clock;
	}

	@Entity(name = "PrecisionQueryClock")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Row {
		@Id int id;
		@Convert(converter = ClockConverter.class)
		@Mutability(ClockMutability.class)
		@Column(secondPrecision = 3)
		Clock clock;
	}

	static class Clock implements Serializable {
		LocalDateTime value;
		Clock(LocalDateTime value) { this.value = value; }
		@Override public boolean equals(Object other) { return other instanceof Clock clock && value.equals( clock.value ); }
		@Override public int hashCode() { return value.hashCode(); }
	}

	public static class ClockMutability extends MutableMutabilityPlan<Clock> {
		@Override protected Clock deepCopyNotNull(Clock value) { return new Clock( value.value ); }
	}

	@Converter
	public static class ClockConverter implements AttributeConverter<Clock, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		@Override public LocalDateTime convertToDatabaseColumn(Clock value) {
			writes.incrementAndGet();
			return value == null ? null : value.value;
		}
		@Override public Clock convertToEntityAttribute(LocalDateTime value) { return value == null ? null : new Clock( value ); }
	}

	/// Serialize both keys and result entries, as a remote cache would.
	///
	/// @author Steve Ebersole
	public static class SerializingQueries extends CachingRegionFactory {
		@Override
		protected StorageAccess createQueryResultsRegionStorageAccess(String name, SessionFactoryImplementor factory) {
			return new MapStorageAccessImpl() {
				@Override public Object getFromCache(Object key, SharedSessionContractImplementor session) {
					return copy( super.getFromCache( copy( key ), session ) );
				}
				@Override public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
					super.putIntoCache( copy( key ), copy( value ), session );
				}
			};
		}
		private static Object copy(Object value) {
			if ( value == null ) { return null; }
			try {
				final var bytes = new ByteArrayOutputStream();
				try ( var out = new ObjectOutputStream( bytes ) ) { out.writeObject( value ); }
				try ( var in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) { return in.readObject(); }
			}
			catch (Exception e) { throw new AssertionError( "Query cache entry must survive serialization", e ); }
		}
	}
}
