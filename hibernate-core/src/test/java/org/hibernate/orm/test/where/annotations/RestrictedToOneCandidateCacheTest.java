package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.query_cache_layout", value = "FULL")
})
class RestrictedToOneCandidateCacheTest extends AbstractRestrictedToOneCandidateTest {
	static Stream<Arguments> cacheCases() {
		return Stream.of( ManyLazy.class, InverseLazy.class ).flatMap( type ->
				Stream.of( false, true ).map( joinFetch -> Arguments.of( type, joinFetch ) ) );
	}

	@ParameterizedTest(name = "{0}, join fetch: {1}")
	@MethodSource("cacheCases")
	void queryCacheSeparatesFilterParameters(Class<? extends Owner> type, boolean joinFetch, SessionFactoryScope scope) {
		assertQueryCacheSeparatesFilterParameters( type, joinFetch, scope );
	}

	static void assertQueryCacheSeparatesFilterParameters(Class<? extends Owner> type, boolean joinFetch, SessionFactoryScope scope) {
		scope.getSessionFactory().getStatistics().clear();
		for ( String language : new String[] { "en", "nl", "fr", "en", "nl", "fr" } ) {
			scope.inTransaction( session -> {
				enable( session, language );
				final Owner owner = session.createSelectionQuery( "from " + type.getSimpleName()
						+ " o" + (joinFetch ? " left join fetch o.filtered left join fetch o.english" : "") + " where o.id=1", type )
						.setCacheable( true ).getSingleResult();
				assertTarget( owner.getFiltered(), language.equals( "en" ) ? Long.valueOf( 101 ) : language.equals( "nl" ) ? Long.valueOf( 102 ) : null );
				assertTarget( owner.getEnglish(), 101L );
			} );
		}
		assertThat( scope.getSessionFactory().getStatistics().getQueryCacheHitCount() ).isEqualTo( 3 );
	}
}
