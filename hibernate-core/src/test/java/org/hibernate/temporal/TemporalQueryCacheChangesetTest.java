package org.hibernate.temporal;

import java.math.BigInteger;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.sql.exec.internal.TemporalJdbcParameter;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@JiraKey("HHH-21226")
@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {TemporalQueryCacheChangesetTest.Article.class, TemporalQueryCacheChangesetTest.Country.class})
@ServiceRegistry(settings = {
		@Setting(name = USE_QUERY_CACHE, value = "true"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.TemporalQueryCacheChangesetTest$Changesets")
})
class TemporalQueryCacheChangesetTest {
	@Test
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class,
			reason = "The changeset id is a numeric column, which Spanner PostgreSQL does not allow in the primary key of the history table")
	void queryCacheRespectsChangeset(SessionFactoryScope scope) {
		final var first = scope.fromTransaction( session -> {
			var article = new Article();
			article.id = 1;
			article.title = "before";
			session.persist( article );
			return session.getCurrentChangesetIdentifier();
		} );
		final var second = scope.fromTransaction( session -> {
			session.find( Article.class, 1 ).title = "after";
			return session.getCurrentChangesetIdentifier();
		} );

		final var factory = scope.getSessionFactory();
		final String hql = "select a.title from Article a where a.id = :id";
		try (var session = factory.withOptions().atChangeset( first ).openSession()) {
			assertEquals( "before", session.createSelectionQuery( hql, String.class )
					.setParameter( "id", 1 ).setCacheable( true ).getSingleResult() );
		}
		try (var session = factory.withOptions().atChangeset( second ).openSession()) {
			assertEquals( "after", session.createSelectionQuery( hql, String.class )
					.setParameter( "id", 1 ).setCacheable( false ).getSingleResult() );
			assertEquals( "after", session.createSelectionQuery( hql, String.class )
					.setParameter( "id", 1 ).setCacheable( true ).getSingleResult() );
		}
		final var hits = factory.getStatistics().getQueryCacheHitCount();
		try (var session = factory.withOptions().atChangeset( first ).openSession()) {
			assertEquals( "before", session.createSelectionQuery( hql, String.class )
					.setParameter( "id", 1 ).setCacheable( true ).getSingleResult() );
		}
		assertEquals( hits + 1, factory.getStatistics().getQueryCacheHitCount() );
	}

	@Test
	void currentAsOfParameterSharesCacheKeyAcrossTransactions(SessionFactoryScope scope) {
		final var mapping = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Article.class )
				.getTemporalMapping().getEndingColumnMapping();
		final var current = new TemporalJdbcParameter( mapping, true );
		final var asOf = new TemporalJdbcParameter( mapping );
		final var bindings = QueryParameterBindingsImpl.EMPTY;
		final var first = scope.fromTransaction( session -> bindings.generateQueryKeyMemento( session, List.of( current ) ) );
		scope.inTransaction( session -> {
			assertEquals( first, bindings.generateQueryKeyMemento( session, List.of( current ) ) );
			assertEquals( first, bindings.generateQueryKeyMemento( session, List.of( asOf ) ) );
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( BigInteger.TWO ).openSession()) {
			assertEquals( first, bindings.generateQueryKeyMemento( session, List.of( current ) ) );
			assertNotEquals( first, bindings.generateQueryKeyMemento( session, List.of( asOf ) ) );
		}
	}

	@Test
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class,
			reason = "The changeset id is a numeric column, which Spanner PostgreSQL does not allow in the primary key of the history table")
	void mappedChangesetTypeDeterminesCacheKey(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var article = new Article();
			article.id = 2;
			article.title = "second article";
			session.persist( article );
			return (BigInteger) session.getCurrentChangesetIdentifier();
		} );
		final var factory = scope.getSessionFactory();
		final String hql = "select a.title from Article a where a.id = 2";
		try (var session = factory.withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( "second article", session.createSelectionQuery( hql, String.class )
					.setCacheable( true ).getSingleResult() );
		}
		final long hits = factory.getStatistics().getQueryCacheHitCount();
		try (var session = factory.withOptions().atChangeset( new ChangesetId( changeset ) ).openSession()) {
			assertEquals( "second article", session.createSelectionQuery( hql, String.class )
					.setCacheable( true ).getSingleResult() );
		}
		assertEquals( hits + 1, factory.getStatistics().getQueryCacheHitCount() );
	}

	@Test
	void nonTemporalQuerySharesCachedResultAcrossChangesets(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var country = new Country();
			country.id = 1;
			session.persist( country );
		} );
		final var factory = scope.getSessionFactory();
		final String hql = "select c.id from Country c";
		try (var session = factory.withOptions().atChangeset( BigInteger.ONE ).openSession()) {
			assertEquals( 1, session.createSelectionQuery( hql, Integer.class ).setCacheable( true ).getSingleResult() );
		}
		final long hits = factory.getStatistics().getQueryCacheHitCount();
		try (var session = factory.withOptions().atChangeset( BigInteger.TWO ).openSession()) {
			assertEquals( 1, session.createSelectionQuery( hql, Integer.class ).setCacheable( true ).getSingleResult() );
		}
		assertEquals( hits + 1, factory.getStatistics().getQueryCacheHitCount() );
	}

	public static class Changesets implements ChangesetIdentifierSupplier<BigInteger> {
		private int next;

		@Override
		public BigInteger generateIdentifier(SharedSessionContract session) {
			return BigInteger.valueOf( ++next );
		}
	}

	static class ChangesetId extends BigInteger {
		ChangesetId(BigInteger id) {
			super( id.toByteArray() );
		}
	}

	@Entity(name = "Country")
	static class Country {
		@Id int id;
	}

	@Entity(name = "Article")
	@Temporal
	static class Article {
		@Id int id;
		String title;
	}

}
