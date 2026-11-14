package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.annotations.MutableClock;
import org.hibernate.orm.test.annotations.MutableClockSettingProvider;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Ordinary generated attributes retain their lifecycle with preservation enabled;
/// version generation alone does not exercise this path.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
		provider = MutableClockSettingProvider.class))
@DomainModel(annotatedClasses = TemporalEnabledGenerationTests.Event.class)
@SessionFactory(useCollectingStatementInspector = true)
class TemporalEnabledGenerationTests {
	@Test
	void creationUpdateReloadAndNoOp(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
		final MutableClock clock = CurrentTimestampGeneration.getClock( scope.getSessionFactory() );
		clock.setInstant( Instant.parse( "2024-02-29T12:00:00Z" ) );
		try {
			final var saved = scope.fromTransaction( session -> {
				final var row = new Event(); row.payload = "first"; session.persist( row ); return row;
			} );
			assertThat( saved.created ).isEqualTo( clock.instant() );
			assertThat( saved.updated ).isEqualTo( clock.instant() );
			assertThat( saved.serverCreated ).isNotNull();
			assertThat( saved.serverUpdated ).isNotNull();
			clock.tick();
			final var updated = scope.fromTransaction( session -> {
				final var row = session.find( Event.class, saved.id ); row.payload = "second"; return row;
			} );
			assertThat( updated.created ).isEqualTo( saved.created );
			assertThat( updated.updated ).isEqualTo( clock.instant() );
			assertThat( updated.serverCreated ).isEqualTo( saved.serverCreated );
			assertThat( updated.serverUpdated ).isAfterOrEqualTo( saved.serverUpdated );
			clock.tick();
			scope.getCollectingStatementInspector().clear();
			scope.inTransaction( session -> {
				final var row = session.find( Event.class, saved.id );
				assertThat( row.updated ).isEqualTo( updated.updated );
				assertThat( row.serverUpdated ).isEqualTo( updated.serverUpdated );
				assertThat( row.payload ).isEqualTo( "second" );
			} );
			assertThat( scope.getCollectingStatementInspector().getSqlQueries() )
					.noneMatch( sql -> sql.toLowerCase( java.util.Locale.ROOT ).startsWith( "update " ) );
		}
		finally { scope.dropData(); clock.reset(); }
	}

	@Entity(name = "EnabledGeneratedEvent")
	static class Event {
		@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
		String payload;
		@CreationTimestamp Instant created;
		@UpdateTimestamp Instant updated;
		@CreationTimestamp(source = SourceType.DB) LocalDateTime serverCreated;
		@UpdateTimestamp(source = SourceType.DB) LocalDateTime serverUpdated;
	}
}
