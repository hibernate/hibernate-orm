/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import org.hibernate.EnabledFetchProfile;
import org.hibernate.Hibernate;
import org.hibernate.Locking;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
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

	// todo : generally, we do not lock collection tables - HHH-19513 plus maybe general problem with many-to-many tables

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testFind(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testFindWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		// note that this is not strictly spec compliant as it says EXTENDED should extend the locks to the `book_genres` table...
		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			// For strict compliance, EXTENDED here should lock `book_genres` but we do not
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
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
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), true );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
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

			if ( session.getDialect().supportsOuterJoinForUpdate() ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS, BOOK_GENRES );

				TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
				TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), true );
			}
			else {
				// should be 3, but follow-on locking is not locking collection tables...
				// todo : track this down - HHH-19513
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), BOOKS );

				// todo : track this down - HHH-19513
				//Helper.checkSql( sqlCollector.getSqlQueries().get( 2 ), session.getDialect(), BOOK_GENRES );
				//TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), true );
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testLock(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.lock( theTalisman, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );

			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testLockWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.lock( theTalisman, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			// Again, for strict compliance, EXTENDED here should lock `book_genres` but we do not
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testRefresh(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.refresh( theTalisman, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testRefreshWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			sqlCollector.clear();
			session.refresh( theTalisman, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), BOOKS );
			TransactionUtil.assertRowLock( factoryScope, BOOKS.getTableName(), "title", "id", theTalisman.getId(), true );
			// Again, for strict compliance, EXTENDED here should lock `book_genres` but we do not
			TransactionUtil.assertRowLock( factoryScope, BOOK_GENRES.getTableName(), "genre", "book_fk", theTalisman.getId(), false );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFind(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS );
			TransactionUtil.assertRowLock( factoryScope, REPORTS.getTableName(), "title", "id", report.getId(), true );
			TransactionUtil.assertRowLock( factoryScope, REPORT_LABELS.getTableName(), "txt", "report_fk", report.getId(), willAggressivelyLockJoinedTables( session.getDialect() ) );
			TransactionUtil.assertRowLock( factoryScope, PERSONS.getTableName(), "name", "id", report.getReporter().getId(), willAggressivelyLockJoinedTables( session.getDialect() ) );
		} );
	}

	private boolean willAggressivelyLockJoinedTables(Dialect dialect) {
		// true when we have something like:
		//
		//		select ...
		//		from books b
		//			join persons p on ...
		//		for update
		///
		// and the database extends for-update to `persons`
		//
		// todo : this is something we should consider and disallow the situation

		return dialect.getLockingSupport().getMetadata().getOuterJoinLockingType() == OuterJoinLockingType.FULL;
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFindWithExtended(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, EXTENDED );
			if ( session.getDialect().supportsOuterJoinForUpdate() ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS, REPORT_LABELS );
				TransactionUtil.assertRowLock( factoryScope, REPORTS.getTableName(), "title", "id", report.getId(), true );
				TransactionUtil.assertRowLock( factoryScope, PERSONS.getTableName(), "name", "id", report.getReporter().getId(),
						willAggressivelyLockJoinedTables( session.getDialect() ) );
				TransactionUtil.assertRowLock( factoryScope, REPORT_LABELS.getTableName(), "txt", "report_fk", report.getId(), true );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), REPORTS );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 2 ), session.getDialect(), PERSONS );
				TransactionUtil.assertRowLock( factoryScope, REPORTS.getTableName(), "title", "id", report.getId(), true );

				// these should happen but currently do not - follow-on locking is not locking element-collection tables...
				// todo : track this down - HHH-19513
				//Helper.checkSql( sqlCollector.getSqlQueries().get( 2 ), session.getDialect(), REPORT_LABELS );
				//TransactionUtil.assertRowLock( factoryScope, REPORT_LABELS.getTableName(), "txt", "report_fk", report.getId(), true );

				// this one should not happen at all.  follow-on locking is not understanding scope probably..
				// todo : track this down - HHH-19514
				TransactionUtil.assertRowLock( factoryScope, PERSONS.getTableName(), "name", "id", report.getReporter().getId(), true );
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFindWithFetchScope(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES );

			if ( session.getDialect().supportsOuterJoinForUpdate() ) {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), REPORTS, REPORT_LABELS, JOINED_REPORTER );
				TransactionUtil.assertRowLock( factoryScope, REPORTS.getTableName(), "title", "id", report.getId(), true );
				TransactionUtil.assertRowLock( factoryScope, PERSONS.getTableName(), "name", "id", report.getReporter().getId(), true );
				TransactionUtil.assertRowLock( factoryScope, REPORT_LABELS.getTableName(), "txt", "report_fk", report.getId(), true );
			}
			else {
				assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 1 ), session.getDialect(), REPORTS );
				Helper.checkSql( sqlCollector.getSqlQueries().get( 2 ), session.getDialect(), PERSONS );
				TransactionUtil.assertRowLock( factoryScope, REPORTS.getTableName(), "title", "id", report.getId(), true );

				// these should happen but currently do not - follow-on locking is not locking element-collection tables...
				// todo : track this down - HHH-19513
				//Helper.checkSql( sqlCollector.getSqlQueries().get( 2 ), session.getDialect(), REPORT_LABELS );
				//TransactionUtil.assertRowLock( factoryScope, REPORT_LABELS.getTableName(), "txt", "report_fk", report.getId(), true );

				// this one should not happen at all.  follow-on locking is not understanding scope probably..
				// todo : track this down - HHH-19514
				TransactionUtil.assertRowLock( factoryScope, PERSONS.getTableName(), "name", "id", report.getReporter().getId(), true );
			}
		} );
	}
}
