/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.orm.AsyncExecutor;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class Helper {
	static void createTestData(SessionFactoryScope factoryScope) {
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
	}

	enum Table {
		BOOKS,
		BOOK_TAGS,
		BOOK_AUTHORS,
		PUBLISHER;

		public String getTableName() {
			return switch ( this ) {
				case BOOKS -> "books";
				case BOOK_TAGS -> "book_tags";
				case BOOK_AUTHORS -> "book_authors";
				case PUBLISHER -> "publishers";
			};
		}

		public String getTableAlias() {
			return switch ( this ) {
				case BOOKS -> "b1_0";
				case BOOK_TAGS -> "t1_0";
				case BOOK_AUTHORS -> "a1_0";
				case PUBLISHER -> "p1_0";
			};
		}

		public String[] getKeyColumnAliases() {
			return switch ( this ) {
				case BOOKS -> new String[] {"b1_0.id"};
				case BOOK_TAGS -> new String[] {"t1_0.book_fk"};
				case BOOK_AUTHORS -> new String[] {"a1_0.book_fk"};
				case PUBLISHER -> new String[] {"p1_0.id"};
			};
		}
	}
	static void checkSql(String sql, Dialect dialect, Table... tablesFetched) {
		// note: assume `tables` is in order
		final PessimisticLockStyle pessimisticLockStyle = dialect.getPessimisticLockStyle();
		if ( pessimisticLockStyle == PessimisticLockStyle.CLAUSE ) {
			final String aliases;
			final RowLockStrategy rowLockStrategy = dialect.getReadRowLockStrategy();
			if ( rowLockStrategy == RowLockStrategy.NONE ) {
				aliases = "";
			}
			else if ( rowLockStrategy == RowLockStrategy.TABLE ) {
				final StringBuilder buffer = new StringBuilder();
				boolean firstPass = true;
				for ( Table table : tablesFetched ) {
					if ( firstPass ) {
						firstPass = false;
					}
					else {
						buffer.append( "," );
					}
					buffer.append( table.getTableAlias() );
				}
				aliases = buffer.toString();
			}
			else {
				final StringBuilder buffer = new StringBuilder();
				boolean firstPass = true;
				for ( Table table : tablesFetched ) {
					if ( firstPass ) {
						firstPass = false;
					}
					else {
						buffer.append( "," );
					}
					buffer.append( StringHelper.join( ",", table.getKeyColumnAliases() ) );
				}
				aliases = buffer.toString();
			}

			final String writeLockString = dialect.getWriteLockString( aliases, Timeouts.WAIT_FOREVER );
			assertThat( sql ).endsWith( writeLockString );
		}
		else {
			// Transact SQL (mssql, sybase) "table hint"-style locking
			final LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
			for ( Table table : tablesFetched ) {
				final String booksTableReference = dialect.appendLockHint( lockOptions, table.getTableName() );
				assertThat( sql ).contains( booksTableReference );
			}
		}
	}

	static void deleteFromTable(SessionFactoryScope factoryScope, String tableName, boolean expectingToBlock) {
		try {
			AsyncExecutor.executeAsync( 2, TimeUnit.SECONDS, () -> {
				factoryScope.inTransaction( (session) -> {
					//noinspection deprecation
					session.createNativeQuery( "delete from " + tableName ).executeUpdate();
					if ( expectingToBlock ) {
						fail( "Expecting delete from " + tableName + " to block dues to locks" );
					}
				} );
			} );
		}
		catch (AsyncExecutor.TimeoutException expected) {
			if ( !expectingToBlock ) {
				fail( "Expecting delete from " + tableName + " succeed, but failed (presumably due to locks)" );
			}
		}
	}
}
