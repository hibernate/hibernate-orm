package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Cache reconstruction enforces representation and target semantics while
/// using exact seconds on both sides of the New York DST transition.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_reference_entries", value = "true")
})
@DomainModel(annotatedClasses = TemporalCacheTests.Event.class)
@SessionFactory(generateStatistics = true)
class TemporalCacheTests {
	@ParameterizedTest
	@ValueSource(strings = { "2026-03-08T06:59:59Z", "2026-03-08T07:00:00Z", "2026-03-08T06:59:59.123456Z" })
	void cacheReconstructsEachInstant(String instant, SessionFactoryScope scope) {
		final var input = Instant.parse( instant ).atZone( ZoneId.of( "Europe/Paris" ) );
		final var event = new Event();
		event.id = 1L;
		event.adjusted = input;
		event.unadjusted = input;
		TemporalCacheAssertions.verify( scope, Event.class, event, event.id, input, DateTimeUtils.adjustToDefaultPrecision( input.toInstant(),
						scope.getSessionFactory().getJdbcServices().getDialect() ),
				row -> row.adjusted, row -> row.unadjusted );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Separate rounding regression below")
	void databaseRoundingAndBothCachePopulationPaths(SessionFactoryScope scope) {
		cacheReconstructsEachInstant( "2026-03-08T06:59:59.123456789Z", scope );
	}

	@Test
	@RequiresDialect(SybaseASEDialect.class)
	@FailureExpected(jiraKey = "HHH-21087", reason = "Sybase driver truncates microseconds while Dialect predicts rounding; separate follow-up")
	void sybaseDatabaseRoundingAndBothCachePopulationPaths(SessionFactoryScope scope) {
		cacheReconstructsEachInstant( "2026-03-08T06:59:59.123456789Z", scope );
	}

	@Entity(name = "TemporalCacheEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	@Immutable
	@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
	static class Event {
		@Id Long id;
		@TimeZoneAdjustment("America/New_York")
		ZonedDateTime adjusted;
		ZonedDateTime unadjusted;
	}
}
