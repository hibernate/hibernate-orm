/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.Locking;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
public class FetchLockingTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		Helper.createTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFindWithFetchLocking(SessionFactoryScope factoryScope) {
		// Here we will lock `books`, and also `publishers` since it is fetched
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3,
					LockModeType.PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );

			// The `book_authors` table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_authors", false );

			// The `book_tags` table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_tags", false );

			// The `books` table should be locked.
			Helper.deleteFromTable( factoryScope, "books", true );
		} );
	}

	@Test
	void testQueryJoiningTagsWithFetchLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			session.createSelectionQuery( "select b from Book b inner join fetch b.tags" )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_FETCHES )
					.list();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(),
					Helper.Table.BOOKS, Helper.Table.BOOK_TAGS );

			// The `book_authors` table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_authors", false );

			// The `book_tags` table should be locked.
			Helper.deleteFromTable( factoryScope, "book_tags", true );

			// The `books` table should be locked.
			Helper.deleteFromTable( factoryScope, "books", true );
		} );

	}
}
