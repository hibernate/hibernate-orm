package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;
import jakarta.persistence.Converter;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Precision belongs to the mapping, not the Java class. Fresh cache/database
/// loads agree while the assigned managed values retain their original fractions.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true")
})
@DomainModel(annotatedClasses = { TemporalPrecisionLifecycleTests.Event.class, TemporalPrecisionLifecycleTests.ConvertedEvent.class, TemporalPrecisionLifecycleTests.ConvertedCollection.class })
@SessionFactory(generateStatistics = true)
class TemporalPrecisionLifecycleTests {
	private static final LocalDateTime INPUT = LocalDateTime.parse( "2026-03-08T12:34:56.123456789" );

	@Test
	void mappingLocalStorageCacheAndDirtyChecking(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		final var stats = factory.getStatistics();
		final var event = new Event(); event.id = 1; event.millis = INPUT; event.micros = INPUT;
		event.natural = INPUT;
		try {
			scope.inTransaction( session -> {
				session.persist( event );
				session.flush();
				assertThat( event.millis ).isEqualTo( INPUT );
				assertThat( event.micros ).isEqualTo( INPUT );
				stats.clear();
				event.millis = INPUT.withNano( 123_499_999 );
				session.flush();
				assertThat( stats.getEntityUpdateCount() ).isZero();
			} );
			stats.clear();
			scope.inTransaction( session -> assertCanonical( session.find( Event.class, 1 ) ) );
			assertThat( stats.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( stats.getEntityLoadCount() ).isZero();
			factory.getCache().evictEntityData( Event.class );
			stats.clear();
			scope.inTransaction( session -> {
				assertCanonical( session.find( Event.class, 1 ) );
				session.doWork( connection -> {
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
							"select millis,micros from PrecisionEvent where id=1" ) ) {
						assertThat( rows.next() ).isTrue();
						assertThat( rows.getObject( 1, LocalDateTime.class ) ).isEqualTo( INPUT.withNano( 123_000_000 ) );
						assertThat( rows.getObject( 2, LocalDateTime.class ) ).isEqualTo( INPUT.withNano( 123_457_000 ) );
					}
				} );
			} );
			assertThat( stats.getEntityLoadCount() ).isEqualTo( 1 );
			assertThat( stats.getSecondLevelCachePutCount() ).isEqualTo( 1 );
			stats.clear();
			scope.inTransaction( session -> assertCanonical( session.find( Event.class, 1 ) ) );
			assertThat( stats.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( stats.getEntityLoadCount() ).isZero();
			scope.inTransaction( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.millis = INPUT.withNano( 123_500_000 );
				session.flush();
				assertThat( stats.getEntityUpdateCount() ).isEqualTo( 1 );
			} );
			factory.getCache().evictEntityData( Event.class );
			scope.inTransaction( session -> assertThat( session.find( Event.class, 1 ).millis )
					.isEqualTo( INPUT.withNano( 124_000_000 ) ) );
		}
		finally { scope.dropData(); }
	}

	@Test
	void naturalIdRetainsExistingType(SessionFactoryScope scope) {
		final var entity = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Event.class );
		assertThat( TemporalPrecisionBasicType.find( entity.findAttributeMapping( "natural" ).getSingleJdbcMapping() ) ).isNull();
		assertThat( TemporalPrecisionBasicType.find( entity.findAttributeMapping( "millis" ).getSingleJdbcMapping() ) ).isNotNull();
	}

	@Test
	void convertedValueIsPreparedOnceAndReusedForCache(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		final var stats = factory.getStatistics();
		final var event = new ConvertedEvent();
		event.id = 2;
		event.stamp = INPUT.toString();
		TextTimestamp.writes.set( 0 );
		TextTimestamp.reads.set( 0 );
		try {
			scope.inTransaction( session -> session.persist( event ) );
			assertThat( TextTimestamp.writes ).hasValue( 1 );
			assertThat( event.stamp ).isEqualTo( INPUT.toString() );
			stats.clear();
			scope.inTransaction( session -> assertThat( session.find( ConvertedEvent.class, 2 ).stamp )
					.isEqualTo( INPUT.withNano( 123_000_000 ).toString() ) );
			assertThat( stats.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( TextTimestamp.writes ).hasValue( 1 );
			assertThat( TextTimestamp.reads ).hasValue( 1 );
			factory.getCache().evictEntityData( ConvertedEvent.class );
			scope.inTransaction( session -> assertThat( session.find( ConvertedEvent.class, 2 ).stamp )
					.isEqualTo( INPUT.withNano( 123_000_000 ).toString() ) );
			assertThat( TextTimestamp.writes ).hasValue( 1 );
			assertThat( TextTimestamp.reads ).hasValue( 2 );
			scope.inTransaction( session -> {
				final var loaded = session.find( ConvertedEvent.class, 2 );
				loaded.stamp = INPUT.withNano( 456_789_123 ).toString();
			} );
			assertThat( TextTimestamp.writes ).hasValue( 2 );
			scope.inTransaction( session -> assertThat( session.find( ConvertedEvent.class, 2 ).stamp )
					.isEqualTo( INPUT.withNano( 457_000_000 ).toString() ) );
			assertThat( TextTimestamp.writes ).hasValue( 2 );
		}
		finally {
			scope.dropData();
		}
	}

	@Test
	void cacheLoadRetainsConvertedValueForPartialUpdate(SessionFactoryScope scope) {
		final var event = new ConvertedEvent();
		event.id = 3;
		event.stamp = INPUT.toString();
		TextTimestamp.writes.set( 0 );
		try {
			scope.inTransaction( session -> session.persist( event ) );
			scope.inTransaction( session -> session.find( ConvertedEvent.class, 3 ).description = "changed" );
			assertThat( TextTimestamp.writes ).hasValue( 1 );
			scope.inTransaction( session -> {
				final var loaded = session.find( ConvertedEvent.class, 3 );
				assertThat( loaded.stamp ).isEqualTo( INPUT.withNano( 123_000_000 ).toString() );
				assertThat( loaded.description ).isEqualTo( "changed" );
			} );
		}
		finally { scope.dropData(); }
	}

	@Test
	void convertedCollectionRetainsEachOccurrence(SessionFactoryScope scope) {
		final var event = new ConvertedCollection();
		event.id = 4;
		event.stamps.add( INPUT.toString() );
		event.stamps.add( INPUT.plusSeconds( 1 ).toString() );
		event.stamps.add( INPUT.toString() );
		try {
			scope.inTransaction( session -> session.persist( event ) );
			TextTimestamp.writes.set( 0 );
			scope.inTransaction( session -> assertThat( session.find( ConvertedCollection.class, 4 ).stamps )
					.containsExactly( INPUT.withNano( 123_000_000 ).toString(),
							INPUT.plusSeconds( 1 ).withNano( 123_000_000 ).toString(),
							INPUT.withNano( 123_000_000 ).toString() ) );
			assertThat( TextTimestamp.writes ).hasValue( 0 );
		}
		finally { scope.dropData(); }
	}

	@Entity(name = "ConvertedPrecisionCollection")
	static class ConvertedCollection {
		@Id int id;
		@ElementCollection
		@OrderColumn
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@Convert(converter = TextTimestamp.class)
		@Column(secondPrecision = 3) List<String> stamps = new ArrayList<>();
	}

	@Entity(name = "ConvertedPrecisionEvent")
	@DynamicUpdate
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class ConvertedEvent {
		@Id int id;
		@Version int version;
		String description;
		@Convert(converter = TextTimestamp.class)
		@Column(secondPrecision = 3) String stamp;
	}

	/// Invocation counts distinguish JDBC preparation from cache publication.
	///
	/// @author Steve Ebersole
	@Converter
	@Immutable
	public static class TextTimestamp implements AttributeConverter<String, LocalDateTime> {
		static final AtomicInteger writes = new AtomicInteger();
		static final AtomicInteger reads = new AtomicInteger();

		@Override
		public LocalDateTime convertToDatabaseColumn(String value) {
			writes.incrementAndGet();
			return value == null ? null : LocalDateTime.parse( value );
		}

		@Override
		public String convertToEntityAttribute(LocalDateTime value) {
			reads.incrementAndGet();
			return value == null ? null : value.toString();
		}
	}

	private static void assertCanonical(Event event) {
		assertThat( event.millis ).isEqualTo( INPUT.withNano( 123_000_000 ) );
		assertThat( event.micros ).isEqualTo( INPUT.withNano( 123_457_000 ) );
	}

	@Entity(name = "PrecisionEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		@Column(secondPrecision = 3) LocalDateTime millis;
		@Column(secondPrecision = 6) LocalDateTime micros;
		@NaturalId @Column(name = "natural_value", secondPrecision = 6) LocalDateTime natural;
	}
}
