package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Cache tokens and collection-triggered version changes share the preserving mapping.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservingVersionCacheTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void cacheDirtyCheckingAndStaleDeletion(boolean metadata) throws InterruptedException {
		try ( var scope = ContextScope.open( metadata, false,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ), List.of(), Cached.class ) ) {
			final var original = new Cached();
			scope.tx( session -> session.persist( original ) );
			final var statistics = scope.factory.getStatistics();
			statistics.clear();
			scope.tx( session -> assertThat( session.find( Cached.class, 1 ).stamp ).isEqualTo( original.stamp ) );
			scope.tx( session -> assertThat( session.find( Cached.class, 1 ).stamp ).isEqualTo( original.stamp ) );
			assertThat( statistics.getSecondLevelCacheHitCount() ).isPositive();
			assertThat( statistics.getEntityUpdateCount() ).isZero();

			Thread.sleep( 15 );
			scope.tx( session -> session.find( Cached.class, 1 ).tags.add( "changed" ) );
			final var changed = scope.factory.fromTransaction( session -> session.find( Cached.class, 1 ).stamp );
			assertThat( changed ).isNotEqualTo( original.stamp );
			assertThatThrownBy( () -> scope.tx( session -> session.remove( original ) ) )
					.isInstanceOf( jakarta.persistence.OptimisticLockException.class );

			Thread.sleep( 15 );
			scope.tx( session -> session.lock( session.find( Cached.class, 1 ), LockModeType.OPTIMISTIC_FORCE_INCREMENT ) );
			final Instant forced = scope.factory.fromTransaction( session -> session.find( Cached.class, 1 ).stamp );
			assertThat( forced ).isNotEqualTo( changed );
			scope.tx( session -> session.remove( session.find( Cached.class, 1 ) ) );
			scope.tx( session -> assertThat( session.find( Cached.class, 1 ) ).isNull() );
		}
	}

	@Entity(name = "PreservingCachedVersion")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Cached {
		@Id int id = 1;
		@Version Instant stamp;
		@ElementCollection List<String> tags = new ArrayList<>();
	}
}
