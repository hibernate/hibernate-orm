package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Mapping precision preparation agrees with storage; each cache entry reconstructs
/// the target zone using its own instant, including across a DST transition.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_reference_entries", value = "true")
})
@DomainModel(annotatedClasses = TemporalCachePrecisionTests.Event.class)
@SessionFactory(generateStatistics = true)
class TemporalCachePrecisionTests {
	@ParameterizedTest
	@ValueSource(strings = { "Europe/Paris", "Asia/Kathmandu" })
	void cacheRetainsItsOwnPrecision(String zone, SessionFactoryScope scope) {
		final var metadata = scope.getMetadataImplementor();
		final var precision = TemporalCachePrecisionSupport.inspect( metadata, Event.class );
		assumeTrue( precision.hasExecutionPlan(), "Primary mappings lack an established precision execution plan: " + precision );
		final var input = Instant.parse( "2026-03-08T06:59:59.600Z" ).atZone( ZoneId.of( zone ) );
		final var expected = Instant.parse( scope.getSessionFactory().getJdbcServices().getDialect().getTemporalValueSemantics()
				.precisionAdjustment() == PrecisionAdjustment.TRUNCATE
				? "2026-03-08T06:59:59Z" : "2026-03-08T07:00:00Z" );
		assertThat( expected ).isNotEqualTo( input.toInstant() );
		final var event = new Event();
		event.id = 1L;
		event.adjusted = input;
		event.unadjusted = input;
		TemporalCacheAssertions.verify( scope, Event.class, event, event.id, input, expected,
				row -> row.adjusted, row -> row.unadjusted );
	}

	@Entity(name = "TemporalCachePrecisionEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	@Immutable
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
