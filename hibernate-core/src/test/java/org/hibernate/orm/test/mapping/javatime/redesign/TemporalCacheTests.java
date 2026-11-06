package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Cache reconstruction enforces representation and target semantics while
/// retaining the existing difference between cached and database precision.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_reference_entries", value = "true")
})
@DomainModel(annotatedClasses = TemporalCacheTests.Event.class)
@SessionFactory(generateStatistics = true)
class TemporalCacheTests {
	@Test
	void writePopulatedCacheKeepsPrecisionAndUsesItsOwnInstant(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isDirectReferenceCacheEntriesEnabled() ).isTrue();
		assertThat( scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Event.class ).canUseReferenceCacheEntries() ).isFalse();
		final var input = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneId.of( "Europe/Paris" ) );
		scope.inTransaction( session -> {
			final var event = new Event();
			event.id = 1L;
			event.adjusted = input;
			event.unadjusted = input;
			session.persist( event );
		} );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.adjusted.toInstant() ).isEqualTo( input.toInstant() );
			assertThat( event.adjusted.getZone() ).isEqualTo( ZoneId.of( "America/New_York" ) );
			assertThat( event.adjusted.getOffset() ).isEqualTo( ZoneOffset.ofHours( -5 ) );
			assertThat( event.unadjusted.getZone() ).isEqualTo( input.getOffset() );
		} );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
		scope.getSessionFactory().getCache().evictEntityData( Event.class );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.adjusted.toInstant() ).isEqualTo( Instant.parse( "2026-03-08T07:00:00Z" ) );
			assertThat( event.adjusted.getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.adjusted.toInstant() ).isEqualTo( Instant.parse( "2026-03-08T07:00:00Z" ) );
			assertThat( event.adjusted.getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) );
			session.remove( event );
		} );
	}

	@Entity(name = "TemporalCacheEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	@org.hibernate.annotations.Immutable
	@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
	static class Event {
		@Id Long id;
		@Column(secondPrecision = 0)
		@TimeZoneAdjustment("America/New_York")
		ZonedDateTime adjusted;
		@Column(secondPrecision = 0)
		ZonedDateTime unadjusted;
	}
}
