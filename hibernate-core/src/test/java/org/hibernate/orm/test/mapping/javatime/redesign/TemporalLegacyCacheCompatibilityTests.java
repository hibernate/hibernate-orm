package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// The new contextual temporal cache policy does not alter legacy reference eligibility.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.type.java_time_use_direct_jdbc", value = "false"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_reference_entries", value = "true")
})
@DomainModel(annotatedClasses = TemporalLegacyCacheCompatibilityTests.Event.class)
@SessionFactory(exportSchema = false)
class TemporalLegacyCacheCompatibilityTests {
	@Test
	void legacyColumnRetainsReferenceCacheEligibility(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isDirectReferenceCacheEntriesEnabled() ).isTrue();
		assertThat( scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Event.class ).canUseReferenceCacheEntries() ).isTrue();
	}

	@Entity
	@Immutable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	static class Event {
		@Id Long id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		ZonedDateTime timestamp;
	}
}
