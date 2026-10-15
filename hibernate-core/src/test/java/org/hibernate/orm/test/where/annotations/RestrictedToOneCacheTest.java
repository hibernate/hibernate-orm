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
class RestrictedToOneCacheTest extends RestrictedToOneTest {
	@ParameterizedTest
	@MethodSource("mappings")
	void staticRestrictionsDoNotPopulateUnreadableEntries(Mapping mapping, SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
		prepare( scope, mapping );
		if ( mapping.targetType() == SqlTarget.class ) {
			assertThat( scope.getSessionFactory().getStatistics().getSecondLevelCachePutCount() ).isZero();
			for ( int run = 0; run < 2; run++ ) {
				scope.inTransaction( session -> {
					final Owner owner = session.find( mapping.ownerType(), 1L );
					assertThat( owner.getTarget() ).isNotNull();
					owner.name = "updated";
				} );
			}
			assertThat( scope.getSessionFactory().getStatistics().getSecondLevelCachePutCount() ).isZero();
		}
		else {
			assertThat( scope.getSessionFactory().getStatistics().getSecondLevelCachePutCount() ).isPositive();
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void cacheCannotHideTheStoredReference(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		// Cache entries created with filters disabled must not conceal an active restriction.
		scope.inTransaction( session -> {
			enable( session );
			final Owner owner = session.find( mapping.ownerType(), 2L );
			assertThat( owner.getTarget() ).isNull();
			owner.name = "from cache";
		} );
		assertStoredReference( scope, mapping, 2L, 2L );
		if ( mapping.targetType() == FilterTarget.class ) {
			scope.inTransaction( session -> assertThat( session.find( mapping.ownerType(), 2L )
					.getTarget() ).isNotNull() );
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void queryCacheRetainsThePhysicalKey(Mapping mapping, SessionFactoryScope scope) {
		prepare( scope, mapping );
		scope.getSessionFactory().getStatistics().clear();
		for ( int run = 0; run < 2; run++ ) {
			final boolean update = run == 1;
			scope.inTransaction( session -> {
				enable( session );
				final Owner owner = session.createQuery( "from " + mapping.ownerType().getSimpleName()
						+ " o left join fetch o.target where o.id = 2", mapping.ownerType() )
						.setCacheable( true ).getSingleResult();
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
