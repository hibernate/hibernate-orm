/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
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
 * Tests for {@linkplain org.hibernate.Locking.Scope#INCLUDE_COLLECTIONS} /
 * {@linkplain jakarta.persistence.PessimisticLockScope#EXTENDED}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
public class ExtendedLockingTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		Helper.createTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFindWithExtendedLocking(SessionFactoryScope factoryScope) {
		// NOTE: this is really the same as BaselineTests#testFindWithLocking
		// 		this may technically be a violation of the spec since I think we should
		// 		also be locking the `book_tags` table.  but the current code takes the
		// 		tact that only fetched tables are eligible for locking.  iow, we only
		//		look at the TableGroup(s) which are part of the query
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3,
					LockModeType.PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );


			// The book_authors table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_authors", false );

			// The book_tags table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_tags", false );

			// The book table should be locked.
			Helper.deleteFromTable( factoryScope, "books", true );
		} );
	}

	@Test
	void testQueryJoiningTagsWithExtendedLocking(SessionFactoryScope factoryScope) {
		// NOTE: roughly the same expectations as #testFindWithExtendedLocking,
		//		but here the `book_tags` table should also get locked
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			final Book theTalisman = session.createSelectionQuery( "select b from Book b inner join fetch b.tags where b.id = 3", Book.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.INCLUDE_COLLECTIONS )
					.uniqueResult();
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(),
					Helper.Table.BOOKS, Helper.Table.BOOK_TAGS );

			// The book_authors table should not be locked.
			Helper.deleteFromTable( factoryScope, "book_authors", false );

			// The book_tags table should be locked.
			Helper.deleteFromTable( factoryScope, "book_tags", true );

			// The book table should be locked.
			Helper.deleteFromTable( factoryScope, "books", true );
		} );

	}
}
