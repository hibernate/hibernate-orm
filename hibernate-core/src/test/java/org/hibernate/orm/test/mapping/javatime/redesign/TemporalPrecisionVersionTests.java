package org.hibernate.orm.test.mapping.javatime.redesign;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.EnumSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/// Version publication uses the stored precision without introducing collision
/// rejection or a different generator. A repeated token still permits an update.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true")
})
@DomainModel(annotatedClasses = TemporalPrecisionVersionTests.VersionedEvent.class)
@SessionFactory(generateStatistics = true)
class TemporalPrecisionVersionTests {
	private static final LocalDateTime CANDIDATE = LocalDateTime.parse( "2026-01-02T12:34:56.123456789" );
	private static final LocalDateTime STORED = CANDIDATE.withNano( 123_000_000 );

	@Test
	void versionPublicationAndEqualTokenUpdate(SessionFactoryScope scope) {
		final var row = new VersionedEvent();
		row.id = 1;
		row.message = "initial";
		try {
			scope.inTransaction( session -> {
				session.persist( row );
				session.flush();
				assertThat( row.version ).isEqualTo( STORED );
				row.message = "same transaction";
				session.flush();
				assertThat( row.version ).isEqualTo( STORED );
			} );
			final var stats = scope.getSessionFactory().getStatistics();
			stats.clear();
			scope.inTransaction( session -> {
				final var loaded = session.find( VersionedEvent.class, 1 );
				assertThat( loaded.version ).isEqualTo( STORED );
				assertThat( loaded.message ).isEqualTo( "same transaction" );
				loaded.message = "equal token";
			} );
			assertThat( stats.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			assertThat( stats.getEntityUpdateCount() ).isEqualTo( 1 );
			scope.getSessionFactory().getCache().evictEntityData( VersionedEvent.class );
			scope.inTransaction( session -> {
				final var loaded = session.find( VersionedEvent.class, 1 );
				assertThat( loaded.version ).isEqualTo( STORED );
				assertThat( loaded.message ).isEqualTo( "equal token" );
			} );
		}
		finally {
			scope.dropData();
		}
	}

	@Entity(name = "PrecisionVersionedEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class VersionedEvent {
		@Id int id;
		String message;
		@Version @FixedVersion @Column(secondPrecision = 3) LocalDateTime version;
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@ValueGenerationType(generatedBy = FixedGenerator.class)
	@interface FixedVersion {
	}

	/// Intentionally repeats a token to characterize the unchanged collision policy.
	///
	/// @author Steve Ebersole
	public static class FixedGenerator implements BeforeExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EnumSet.allOf( EventType.class );
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner, Object current, EventType event) {
			return CANDIDATE;
		}
	}
}
