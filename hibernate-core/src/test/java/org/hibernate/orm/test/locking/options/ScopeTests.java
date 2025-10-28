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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
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
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testFind(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), false, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), false, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testFindWithExtended(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), true, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testFindWithExtendedAndFetch(SessionFactoryScope factoryScope) {

		// note that this is not strictly spec compliant as it says EXTENDED should extend
		// the locks to the `book_genres` table...
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find(
					Book.class,
					3,
					PESSIMISTIC_WRITE,
					EXTENDED,
					new EnabledFetchProfile("book-genres")
			);
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), true, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testLock(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			session.lock( theTalisman, PESSIMISTIC_WRITE );

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), false, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), false, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testLockWithExtended(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			session.lock( theTalisman, PESSIMISTIC_WRITE, EXTENDED );

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), true, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testRefresh(SessionFactoryScope factoryScope) {

		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			session.refresh( theTalisman, PESSIMISTIC_WRITE );

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), false, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), false, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "update does not block")
	void testRefreshWithExtended(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book theTalisman = session.find( Book.class, 3 );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			session.refresh( theTalisman, PESSIMISTIC_WRITE, EXTENDED );

			BOOKS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_AUTHORS.checkLocked( theTalisman.getId(), true, factoryScope );
			BOOK_GENRES.checkLocked( theTalisman.getId(), true, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFind(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE );
			REPORTS.checkLocked( report.getId(), true, factoryScope );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFindWithExtended(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, EXTENDED );

			REPORTS.checkLocked( report.getId(), true, factoryScope );
			PERSONS.checkLocked( report.getReporter().getId(), willAggressivelyLockJoinedTables( session.getDialect() ), factoryScope );
			REPORT_LABELS.checkLocked( report.getId(), true, factoryScope );
		} );
	}

	private boolean willAggressivelyLockJoinedTables(Dialect dialect) {
		// Will be true when we have something like:
		//
		//		select ...
		//		from books b
		//			join persons p on ...
		//		for update
		///
		// and the database extends for-update to `persons`

		return dialect.getLockingSupport().getMetadata().getOuterJoinLockingType() == OuterJoinLockingType.FULL;
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "H2 seems to not extend locks across joins")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testEagerFindWithFetchScope(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Report report = session.find( Report.class, 2, PESSIMISTIC_WRITE, Locking.Scope.INCLUDE_FETCHES );

			REPORTS.checkLocked( report.getId(), true, factoryScope );
			PERSONS.checkLocked( report.getReporter().getId(), true, factoryScope );
			REPORT_LABELS.checkLocked( report.getId(), true, factoryScope );
		} );
	}
}
