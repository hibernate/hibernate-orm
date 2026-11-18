package org.hibernate.orm.test.where.annotations;

import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BytecodeEnhanced(testEnhancedClasses = { AbstractRestrictedToOneCandidateTest.ManyLazy.class,
		AbstractRestrictedToOneCandidateTest.OneLazy.class, AbstractRestrictedToOneCandidateTest.InverseLazy.class,
		AbstractRestrictedToOneCandidateTest.Translation.class })
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.query_cache_layout", value = "FULL")
})
class RestrictedToOneCandidateEnhancedTest extends AbstractRestrictedToOneCandidateTest {
	@Test
	void lazyManyToOneRetainsSelectedIdentity(SessionFactoryScope scope) {
		assertLazyIdentity( ManyLazy.class, "find", scope );
	}

	@Test
	void lazyOwningOneToOneRetainsSelectedIdentity(SessionFactoryScope scope) {
		assertLazyIdentity( OneLazy.class, "find", scope );
	}

	@Test
	void lazyInverseOneToOneRetainsSelectedIdentity(SessionFactoryScope scope) {
		assertLazyIdentity( InverseLazy.class, "find", scope );
	}

	@Test
	void emptyFetchGraphRetainsSelectedIdentity(SessionFactoryScope scope) {
		assertLazyIdentity( ManyEager.class, "graphLazy", scope );
	}

	private void assertLazyIdentity(Class<? extends Owner> type, String loading, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var owner = load( session, type, loading, 1L );
			assertThat( Hibernate.isInitialized( owner.getFiltered() ) ).isFalse();
			// The selected id must survive until first access, even if the filter changes.
			enable( session, "en" );
			assertTarget( owner.getFiltered(), 102L );
			assertTarget( owner.getEnglish(), 101L );
		} );
	}

	@Test
	void nativeQueryResolvesInverseCandidate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "nl" );
			final var owner = load( session, InverseLazy.class, "native", 1L );
			assertTarget( owner.getFiltered(), 102L );
			assertTarget( owner.getEnglish(), 101L );
		} );
	}

	@Test
	void queryCachePreservesSelectedLazyTarget(SessionFactoryScope scope) {
		RestrictedToOneCandidateCacheTest.assertQueryCacheSeparatesFilterParameters( ManyLazy.class, false, scope );
	}

	@Test
	void dirtyTrackingPreservesHiddenPhysicalKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			enable( session, "fr" );
			final var owner = session.find( ManyLazy.class, 3L );
			assertTarget( owner.getFiltered(), null );
			assertTarget( owner.getEnglish(), null );
			owner.name = "updated";
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select label_id from candidate_owner where id=3", Long.class ).getSingleResult() ).isEqualTo( 3L ) );
	}
}
