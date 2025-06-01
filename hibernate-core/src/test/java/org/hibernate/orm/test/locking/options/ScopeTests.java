/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import org.hibernate.EnabledFetchProfile;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.OuterJoinLockingLevel;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.orm.test.locking.options.Helper.Table.BOOKS;
import static org.hibernate.orm.test.locking.options.Helper.Table.BOOK_AUTHORS;
import static org.hibernate.orm.test.locking.options.Helper.Table.BOOK_GENRES;
import static org.hibernate.orm.test.locking.options.Helper.Table.JOINED_REPORTER;
import static org.hibernate.orm.test.locking.options.Helper.Table.PERSONS;
import static org.hibernate.orm.test.locking.options.Helper.Table.REPORTS;
import static org.hibernate.orm.test.locking.options.Helper.Table.REPORT_LABELS;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class, Report.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
@Tag("db-locking")
public class ScopeTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		Helper.createTestData( factoryScope );
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
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testFindWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// note that this is not strictly spec compliant as it says EXTENDED should extend
		// the locks to the `book_genres` table...
		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			// For strict compliance, EXTENDED here should lock `book_genres` but we do not
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	@FailureExpected(reason = "See https://hibernate.atlassian.net/browse/HHH-19336?focusedCommentId=121552")
	void testFindWithExtendedJpaExpectation(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			// these 2 assertions would depend a bit on the approach and/or dialect
//			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
//			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOK_GENRES );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	void testFindWithExtendedAndFetch(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// note that this is not strictly spec compliant as it says EXTENDED should extend
		// the locks to the `book_genres` table...
		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find(
					Book.class,
					3,
					PESSIMISTIC_WRITE,
					EXTENDED,
					new EnabledFetchProfile("book-genres")
			);
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS, BOOK_GENRES );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testLock(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.lock( theTalisman, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// since this uses the SimpleSelect approach, we should have `for update`, but not `for update of ...`
			//Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			final String sql = sqlCollector.getSqlQueries().get( 0 );
			if ( session.getDialect().getPessimisticLockStyle() == PessimisticLockStyle.CLAUSE ) {
				assertThat( sql ).endsWith( " for update" );
			}
			else {
				final LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
				final String booksTableReference = session.getDialect().appendLockHint( lockOptions, BOOKS.getTableName() );
				assertThat( sql ).contains( booksTableReference );
			}
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testLockWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.lock( theTalisman, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			// Again, for strict compliance, EXTENDED here should lock `book_genres` but we do not
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testRefresh(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.refresh( theTalisman, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testRefreshWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.lock( theTalisman, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.deleteFromTable( factoryScope, BOOKS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_GENRES.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, BOOK_AUTHORS.getTableName(), false );
		} );
	}

	@Test
	void testEagerFind(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.find( Report.class, 2, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS );
			TransactionUtil.deleteFromTable( factoryScope, REPORTS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, PERSONS.getTableName(), willAggressivelyLockJoinedTables( session.getDialect() ) );
			TransactionUtil.deleteFromTable( factoryScope, REPORT_LABELS.getTableName(), willAggressivelyLockJoinedTables( session.getDialect() ) );
		} );
	}

	private boolean willAggressivelyLockJoinedTables(Dialect dialect) {
		// true when we have something like:
		//		select ...
		//		from books b
		//			join persons p on ...
		//		for update
		// and the database extends for-update to the joins
		//
		// todo : this is something we should consider and disallow the situation

		final OuterJoinLockingLevel outerJoinLockingLevel = dialect.getOuterJoinLockingLevel();
		if ( outerJoinLockingLevel == OuterJoinLockingLevel.FULL ) {
			// there will be a join with some form of locking
			final PessimisticLockStyle pessimisticLockStyle = dialect.getPessimisticLockStyle();
			if ( pessimisticLockStyle == PessimisticLockStyle.CLAUSE ) {
				final RowLockStrategy rowLockStrategy = dialect.getWriteRowLockStrategy();
				if ( rowLockStrategy == RowLockStrategy.NONE ) {
					return true;
				}
			}
		}
		return false;
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	void testEagerFindWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS, REPORT_LABELS );
			TransactionUtil.deleteFromTable( factoryScope, REPORTS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, PERSONS.getTableName(), false );
			TransactionUtil.deleteFromTable( factoryScope, REPORT_LABELS.getTableName(), true );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	void testEagerFindWithFetchScope(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS, REPORT_LABELS, JOINED_REPORTER );
			TransactionUtil.deleteFromTable( factoryScope, REPORTS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, PERSONS.getTableName(), true );
			TransactionUtil.deleteFromTable( factoryScope, REPORT_LABELS.getTableName(), true );
		} );
	}
}
