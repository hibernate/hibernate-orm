/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.query_cache_layout", value = "FULL")
})
class AssociationRestrictedToOneCacheTest extends AssociationRestrictedToOneTest {
	@ParameterizedTest
	@MethodSource("mappings")
	void cachedTargetsDoNotBypassAssociationRestrictions(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		// Join-table inserts may invalidate the owner's initial cache entry. Warm it by loading.
		scope.inTransaction( session -> session.find( mapping.ownerType(), 2L ) );
		final var cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( mapping.targetType(), 2L ) ).isTrue();
		assertThat( cache.containsEntity( mapping.ownerType(), 2L ) )
				.isEqualTo( mapping.ownerType().getSimpleName().contains( "Filter" ) );
		scope.inTransaction( session -> {
			enable( session );
			assertThat( session.find( mapping.targetType(), 2L ) ).isNotNull();
			final Owner owner = session.find( mapping.ownerType(), 2L );
			assertThat( owner.getTarget() ).isNull();
			owner.name = "cached target";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
		if ( mapping.ownerType().getSimpleName().contains( "Filter" ) ) {
			scope.inTransaction( session -> assertThat( session.find( mapping.ownerType(), 2L ).getTarget() ).isNotNull() );
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void queryCachePreservesHiddenKeys(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.getSessionFactory().getStatistics().clear();
		for ( int run = 0; run < 2; run++ ) {
			final boolean update = run == 1;
			scope.inTransaction( session -> {
				enable( session );
				final Owner owner = session.createQuery( "from " + mapping.ownerType().getSimpleName()
						+ " o left join fetch o.target where o.id=2", mapping.ownerType() ).setCacheable( true ).getSingleResult();
				assertThat( owner.getTarget() ).isNull();
				if ( update ) {
					owner.name = "query cache";
				}
			} );
		}
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 1 );
		assertStoredReference( scope, mapping, 2L, 2L );
	}
}
