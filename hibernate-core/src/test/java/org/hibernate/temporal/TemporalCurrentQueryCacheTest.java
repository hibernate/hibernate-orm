package org.hibernate.temporal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21226")
@DomainModel(annotatedClasses = TemporalQueryCacheChangesetTest.Article.class)
@ServiceRegistry(settings = {
		@Setting(name = USE_QUERY_CACHE, value = "true"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
})
@SessionFactory(generateStatistics = true)
class TemporalCurrentQueryCacheTest {
	@Test
	void currentQueryHitsCacheAcrossTransactions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new TemporalQueryCacheChangesetTest.Article();
			article.id = 1;
			article.title = "published";
			session.persist( article );
		} );
		final String hql = "select a.title from Article a where a.id = 1";
		scope.inTransaction( session -> assertEquals( "published", session.createSelectionQuery( hql, String.class )
				.setCacheable( true ).getSingleResult() ) );
		final var statistics = scope.getSessionFactory().getStatistics();
		final long hits = statistics.getQueryCacheHitCount();
		scope.inTransaction( session -> assertEquals( "published", session.createSelectionQuery( hql, String.class )
				.setCacheable( true ).getSingleResult() ) );
		assertEquals( hits + 1, statistics.getQueryCacheHitCount() );

		scope.inTransaction( session -> session.find( TemporalQueryCacheChangesetTest.Article.class, 1 ).title = "revised" );
		scope.inTransaction( session -> assertEquals( "revised", session.createSelectionQuery( hql, String.class )
				.setCacheable( true ).getSingleResult() ) );
	}
}
