/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Follow-on locking builds its locking select from the attributes of the entity being locked.
 * An inverse one-to-one is mapped to a column of the associated table, so attempting to select
 * it raised an {@code UnknownTableReferenceException} against a table that select never joins.
 *
 * @author Ivo Raisr
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Article.class, ArticleReview.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-20744" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
@Tag("db-locking")
public class InverseToOneFollowOnLockingTests {
	private static final Helper.TableInformation ARTICLES = new ArticleTable();

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Article article = new Article( 1, "Follow-on locking" );
			session.persist( article );
			session.persist( new ArticleReview( 1, "Accepted", article ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFind(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.find( Article.class, 1, LockModeType.PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );

			assertLockedArticlesOnly( sqlCollector, session.getDialect() );
		} );
	}

	@Test
	void testQueryWithJoinFetch(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionQuery( "from Article a left join fetch a.review where a.id = 1", Article.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getResultList();

			assertLockedArticlesOnly( sqlCollector, session.getDialect() );
		} );
	}

	@Test
	void testFetchedScopeStillLocksTheAssociatedTable(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionQuery( "from Article a left join fetch a.review where a.id = 1", Article.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setLockScope( PessimisticLockScope.FETCHED )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getResultList();

			if ( usesTableHints( session.getDialect() ) ) {
				return;
			}

			// skipping the inverse one-to-one must not cost the fetched entity its lock; it is
			// locked as an entity in its own right rather than as an attribute of the article
			final List<String> followOnStatements = followOnStatements( sqlCollector );
			assertThat( followOnStatements ).hasSize( 2 );
			assertThat( followOnStatements ).anySatisfy( sql -> assertThat( sql ).contains( "articles" ) );
			assertThat( followOnStatements ).anySatisfy( sql -> assertThat( sql ).contains( "article_reviews" ) );
		} );
	}

	private void assertLockedArticlesOnly(SQLStatementInspector sqlCollector, Dialect dialect) {
		if ( usesTableHints( dialect ) ) {
			// t-sql applies the lock to the initial select as a table hint
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), false, dialect, ARTICLES );
			return;
		}

		final List<String> followOnStatements = followOnStatements( sqlCollector );
		assertThat( followOnStatements ).hasSize( 1 );

		final String lockingSelect = followOnStatements.get( 0 );
		Helper.checkSql( lockingSelect, true, dialect, ARTICLES );
		assertThat( lockingSelect ).doesNotContain( "article_reviews" );
	}

	private List<String> followOnStatements(SQLStatementInspector sqlCollector) {
		final List<String> queries = sqlCollector.getSqlQueries();
		assertThat( queries ).hasSizeGreaterThan( 1 );
		return queries.subList( 1, queries.size() );
	}

	private boolean usesTableHints(Dialect dialect) {
		return dialect.getLockingSupport().getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT;
	}

	private static class ArticleTable implements Helper.TableInformation {
		@Override
		public String getTableName() {
			return "articles";
		}

		@Override
		public String getTableAlias() {
			return "a1_0";
		}

		@Override
		public String getKeyColumnName() {
			return "id";
		}
	}
}
