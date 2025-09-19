/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.transaction.TransactionUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static void createTestData(SessionFactoryScope factoryScope) {
		// create book data
		factoryScope.inTransaction( (session) -> {
			final Person milton = new Person( 1, "John Milton" );
			session.persist( milton );
			final Person campbell = new Person( 2, "Joseph Campbell" );
			session.persist( campbell );
			final Person king = new Person( 3, "Stephen King" );
			session.persist( king );
			final Person straub = new Person( 4, "Peter Straub" );
			session.persist( straub );
			final Person doe = new Person( 5, "John Doe" );
			session.persist( doe );

			final Publisher acme = new Publisher( 1, "Acme Publishing House", doe );
			session.persist( acme );

			final Book paradiseLost = new Book( 1, "Paradise Lost", "Narrative poem, in the epic style, ..." );
			paradiseLost.addAuthor( milton );
			session.persist( paradiseLost );

			final Book thePowerOfMyth = new Book( 2, "The Power of Myth",
					"A look at the themes and symbols of ancient narratives ..." );
			thePowerOfMyth.addAuthor( campbell );
			session.persist( thePowerOfMyth );

			final Book theTalisman = new Book( 3, "The Talisman", "Epic of the struggle between good and evil ...", acme );
			theTalisman.addAuthor( king );
			theTalisman.addAuthor( straub );
			theTalisman.addTag( "Dark fantasy" );
			session.persist( theTalisman );

			final Book theDarkTower = new Book( 4, "The Dark Tower", "The epic final to the series ...", acme );
			theDarkTower.addAuthor( king );
			session.persist( theDarkTower );
		} );

		// create report data
		factoryScope.inTransaction( (session) -> {
			final Person steve = new Person( 6, "Steve" );
			final Person andrea = new Person( 7, "Andrea" );
			final Person gavin = new Person( 8, "Gavin" );
			session.persist( steve );
			session.persist( andrea );
			session.persist( gavin );

			final Report report1 = new Report( 1, steve );
			final Report report2 = new Report( 2, steve, "locking" );
			final Report report3 = new Report( 3, steve, "locking", "pessimistic" );
			final Report report4 = new Report( 4, andrea );
			final Report report5 = new Report( 5, andrea, "locking", "find", "hql" );
			final Report report6 = new Report( 6, gavin, "locking" );
			session.persist( report1 );
			session.persist( report2 );
			session.persist( report3 );
			session.persist( report4 );
			session.persist( report5 );
			session.persist( report6 );
		} );
	}

	public static <T> Set<T> toSet(T[] labels) {
		final HashSet<T> result = new HashSet<>();
		Collections.addAll( result, labels );
		return result;
	}

	public interface TableInformation {
		String getTableName();
		String getTableAlias();
		String getKeyColumnName();

		default String getKeyColumnReference() {
			return getKeyColumnReference( getTableAlias() );
		}

		default String getKeyColumnReference(String tableAlias) {
			return tableAlias + "." + getKeyColumnName();
		}
	}

	public enum Table implements TableInformation {
		BOOKS,
		PERSONS,
		PUBLISHER,
		REPORTS,
		BOOK_GENRES,
		BOOK_AUTHORS,
		REPORT_LABELS,
		JOINED_REPORTER;

		public String getTableName() {
			return switch ( this ) {
				case BOOKS -> "books";
				case PERSONS -> "persons";
				case PUBLISHER -> "publishers";
				case REPORTS, JOINED_REPORTER -> "reports";
				case BOOK_GENRES -> "book_genres";
				case BOOK_AUTHORS -> "book_authors";
				case REPORT_LABELS -> "report_labels";
			};
		}

		public String getTableAlias() {
			return switch ( this ) {
				case BOOKS -> "b1_0";
				case PUBLISHER, PERSONS -> "p1_0";
				case REPORTS -> "r1_0";
				case BOOK_GENRES -> "g1_0";
				case BOOK_AUTHORS -> "a1_0";
				case REPORT_LABELS -> "l1_0";
				case JOINED_REPORTER -> "r2_0";
			};
		}

		public String getKeyColumnName() {
			return switch ( this ) {
				case BOOKS, PERSONS, PUBLISHER, REPORTS, JOINED_REPORTER -> "id";
				case BOOK_GENRES, BOOK_AUTHORS -> "book_fk";
				case REPORT_LABELS -> "report_fk";
			};
		}

		public String getCheckColumnName() {
			return switch ( this ) {
				case BOOKS, REPORTS -> "title";
				case PERSONS, JOINED_REPORTER, PUBLISHER -> "name";
				case REPORT_LABELS -> "txt";
				case BOOK_GENRES -> "genre";
				case BOOK_AUTHORS -> "idx";
			};
		}

		public void checkLocked(Number keyValue, boolean expectedToBeLocked, SessionFactoryScope factoryScope) {
			if ( this == BOOK_AUTHORS ) {
				TransactionUtil.deleteRow( factoryScope, getTableName(), expectedToBeLocked );
			}
			else {
				TransactionUtil.assertRowLock(
						factoryScope,
						getTableName(),
						getCheckColumnName(),
						getKeyColumnName(),
						keyValue,
						expectedToBeLocked
				);
			}
		}
	}

	public static void checkSql(String sql, Dialect dialect, TableInformation... tablesFetched) {
		checkSql( sql, false, dialect, tablesFetched );
	}

	public static void checkSql(String sql, boolean expectingFollowOn, Dialect dialect, TableInformation... tablesFetched) {
		// note: assume `tables` is in order
		final LockingSupport.Metadata lockingMetadata = dialect.getLockingSupport().getMetadata();
		final PessimisticLockStyle pessimisticLockStyle = lockingMetadata.getPessimisticLockStyle();

		if ( pessimisticLockStyle == PessimisticLockStyle.CLAUSE ) {
			final String aliases;
			final RowLockStrategy rowLockStrategy = lockingMetadata.getReadRowLockStrategy();
			if ( rowLockStrategy == RowLockStrategy.NONE ) {
				aliases = "";
			}
			else if ( rowLockStrategy == RowLockStrategy.TABLE ) {
				final StringBuilder buffer = new StringBuilder();
				boolean firstPass = true;
				for ( TableInformation table : tablesFetched ) {
					if ( firstPass ) {
						firstPass = false;
					}
					else {
						buffer.append( "," );
					}
					buffer.append( expectingFollowOn ? "tbl" : table.getTableAlias() );
				}
				aliases = buffer.toString();
			}
			else {
				assert rowLockStrategy == RowLockStrategy.COLUMN;
				final StringBuilder buffer = new StringBuilder();
				boolean firstPass = true;
				for ( TableInformation table : tablesFetched ) {
					if ( firstPass ) {
						firstPass = false;
					}
					else {
						buffer.append( "," );
					}
					buffer.append( expectingFollowOn ? table.getKeyColumnReference( "tbl") : table.getKeyColumnReference() );
				}
				aliases = buffer.toString();
			}

			final String writeLockString = dialect.getWriteLockString( aliases, Timeouts.WAIT_FOREVER );
			assertThat( sql ).endsWith( writeLockString );
		}
		else {
			// Transact SQL (mssql, sybase) "table hint"-style locking
			final LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
			for ( TableInformation table : tablesFetched ) {
				final String tableAlias = expectingFollowOn ? "tbl" : table.getTableAlias();
				final String booksTableReference = dialect.appendLockHint( lockOptions, tableAlias );
				assertThat( sql ).contains( booksTableReference );
			}
		}
	}

}
