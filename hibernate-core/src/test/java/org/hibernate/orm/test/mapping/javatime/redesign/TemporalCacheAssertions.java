package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.assertj.core.api.Assertions.assertThat;

/// Shared lifecycle assertions for isolated, single-entity temporal cache fixtures.
/// Each read uses a new persistence context and statistics distinguish cache hits
/// from database loads. Cleanup is outside the measured phases.
///
/// @author Steve Ebersole
final class TemporalCacheAssertions {
	private TemporalCacheAssertions() {
	}

	static <T> void verify(
			SessionFactoryScope scope, Class<T> entityClass, T entity, long id,
			ZonedDateTime input, Instant databaseInstant,
			Function<T, ZonedDateTime> adjusted, Function<T, ZonedDateTime> unadjusted) {
		final var factory = scope.getSessionFactory();
		assertThat( factory.getSessionFactoryOptions().isDirectReferenceCacheEntriesEnabled() ).isTrue();
		assertThat( factory.getMappingMetamodel().getEntityDescriptor( entityClass ).canUseReferenceCacheEntries() ).isFalse();
		final var statistics = factory.getStatistics();
		try {
			scope.inTransaction( session -> session.persist( entity ) );
			statistics.clear();
			scope.inTransaction( session -> assertValues(
					session.find( entityClass, id ), input, input.toInstant(), adjusted, unadjusted ) );
			assertThat( statistics.getSecondLevelCacheHitCount() ).as( "write-populated cache hit" ).isEqualTo( 1 );
			assertThat( statistics.getEntityLoadCount() ).as( "no database load on write-cache hit" ).isZero();

			factory.getCache().evictEntityData( entityClass );
			statistics.clear();
			scope.inTransaction( session -> assertValues(
					session.find( entityClass, id ), input, databaseInstant, adjusted, unadjusted ) );
			assertThat( statistics.getSecondLevelCacheHitCount() ).as( "evicted cache" ).isZero();
			assertThat( statistics.getEntityLoadCount() ).as( "database load" ).isEqualTo( 1 );
			assertThat( statistics.getSecondLevelCachePutCount() ).as( "database-populated cache" ).isEqualTo( 1 );

			statistics.clear();
			scope.inTransaction( session -> assertValues(
					session.find( entityClass, id ), input, databaseInstant, adjusted, unadjusted ) );
			assertThat( statistics.getSecondLevelCacheHitCount() ).as( "database-populated cache hit" ).isEqualTo( 1 );
			assertThat( statistics.getEntityLoadCount() ).as( "no database load on subsequent cache hit" ).isZero();
		}
		finally {
			try {
				scope.inTransaction( session -> {
					final var row = session.find( entityClass, id );
					if ( row != null ) {
						session.remove( row );
					}
				} );
			}
			finally {
				factory.getCache().evictEntityData( entityClass );
			}
		}
	}

	private static <T> void assertValues(
			T row, ZonedDateTime input, Instant expected,
			Function<T, ZonedDateTime> adjusted, Function<T, ZonedDateTime> unadjusted) {
		assertThat( row ).isNotNull();
		assertValue( adjusted.apply( row ), expected.atZone( ZoneId.of( "America/New_York" ) ) );
		assertValue( unadjusted.apply( row ), expected.atZone( input.getOffset() ) );
	}

	private static void assertValue(ZonedDateTime actual, ZonedDateTime expected) {
		assertThat( actual ).isNotNull();
		assertThat( actual.toInstant() ).isEqualTo( expected.toInstant() );
		assertThat( actual.getZone() ).isEqualTo( expected.getZone() );
		assertThat( actual.getOffset() ).isEqualTo( expected.getOffset() );
		assertThat( actual.toLocalDateTime() ).isEqualTo( expected.toLocalDateTime() );
	}
}
