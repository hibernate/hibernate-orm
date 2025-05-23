/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions based on how things work without locking
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsSelectLocking.class )
public class BaselineTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		Helper.createTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testFindWithLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, LockModeType.PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );

			// The `book_authors` table should not be locked.
			TransactionUtil.deleteFromTable( factoryScope, "book_authors", false );

			// The `book_tags` table should not be locked.
			TransactionUtil.deleteFromTable( factoryScope, "book_tags", false );

			// The `books` table should be locked.
			TransactionUtil.deleteFromTable( factoryScope, "books", true );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	void testQueryJoiningTagsWithLocking(SessionFactoryScope factoryScope) {
		// NOTE: roughly the same expectations as #testFindWithExtendedLocking,
		//		but here the `book_tags` table should also get locked
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			final Book theTalisman = session.createSelectionQuery( "select b from Book b inner join fetch b.tags where b.id = 3", Book.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.uniqueResult();
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();

			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Helper.Table.BOOKS );

			// The `book_authors` table should not be locked.
			TransactionUtil.deleteFromTable( factoryScope, "book_authors", false );

			// Whether the `book_tags` table should be locked or not depends on the capability of the db:
			//		* if the database uses table hints, it will be locked
			//		* if the database supports for-update-of, it will not be locked since `of` will only reference the root table (`books`)
			//		* if the database supports for-update (no -of), it will be locked since it locks all returned rows
			final boolean expectingToBlockTags = session.getDialect().getWriteRowLockStrategy() == RowLockStrategy.NONE
					|| session.getDialect().getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT;
			TransactionUtil.deleteFromTable( factoryScope, "book_tags", expectingToBlockTags );

			// The `books` table should be locked.
			TransactionUtil.deleteFromTable( factoryScope, "books", true );
		} );

	}
}
