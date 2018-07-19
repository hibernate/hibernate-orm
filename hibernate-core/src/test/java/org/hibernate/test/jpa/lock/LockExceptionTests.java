/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.lock;

import java.util.Collections;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.PessimisticLockException;

import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLServerSnapshotIsolationConnectionProvider;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.hibernate.testing.util.ExceptionUtil;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(DialectChecks.SupportNoWait.class)
public class LockExceptionTests extends AbstractJPATest {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		if( SQLServerDialect.class.isAssignableFrom( DIALECT.getClass() )) {
			cfg.getProperties().put( AvailableSettings.CONNECTION_PROVIDER, new SQLServerSnapshotIsolationConnectionProvider() );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8786" )
	public void testLockTimeoutFind() {
		final Item item = new Item( "find" );

		inTransaction(
				session -> session.persist( item )
		);

		try {
			inTransaction(
					session -> {
						session.find( Item.class, item.getId(), LockModeType.PESSIMISTIC_WRITE );

						TransactionUtil2.inTransaction(
								sessionFactory(),
								secondSession -> {
									try {
										secondSession.find(
												Item.class,
												item.getId(),
												LockModeType.PESSIMISTIC_WRITE,
												Collections.singletonMap( AvailableSettings.JPA_LOCK_TIMEOUT, LockOptions.NO_WAIT )
										);
										fail( "Expecting a failure" );
									}
									catch ( LockTimeoutException | PessimisticLockException expected ) {
										// expected outcome
									}
								}
						);
					}
			);
		}
		catch (Exception e) {
			log.error( "Exception thrown", e );
		}
		finally {
			inTransaction(
					session -> session.createQuery( "delete Item" ).executeUpdate()
			);
		}
	}

	@Test
	public void testLockTimeoutRefresh() {
		final Item item = new Item( "refresh" );

		inTransaction(
				session -> session.persist( item )
		);

		try {
			inTransaction(
					session -> {
						session.find( Item.class, item.getId(), LockModeType.PESSIMISTIC_WRITE );

						TransactionUtil2.inTransaction(
								sessionFactory(),
								secondSession -> {
									try {
										// generally speaking we should be able to read the row
										Item item2 = secondSession.get( Item.class, item.getId() );
										secondSession.refresh(
												item2,
												LockModeType.PESSIMISTIC_WRITE,
												Collections.singletonMap( AvailableSettings.JPA_LOCK_TIMEOUT, LockOptions.NO_WAIT )
										);
										fail( "Expecting a failure" );
									}
									catch ( LockTimeoutException | PessimisticLockException expected ) {
										// expected outcome
									}
								}
						);
					}
			);
		}
		finally {
			inTransaction(
					session -> session.createQuery( "delete Item" ).executeUpdate()
			);
		}
	}

	@Test
	public void testLockTimeoutLock() {
		final Item item = new Item( "lock" );

		inTransaction(
				session -> session.persist( item )
		);

		try {
			inTransaction(
					session -> {
						session.find( Item.class, item.getId(), LockModeType.PESSIMISTIC_WRITE );

						TransactionUtil2.inTransaction(
								sessionFactory(),
								secondSession -> {
									try {
										// generally speaking we should be able to read the row
										Item item2 = secondSession.get( Item.class, item.getId() );
										secondSession.lock(
												item2,
												LockModeType.PESSIMISTIC_WRITE,
												Collections.singletonMap( AvailableSettings.JPA_LOCK_TIMEOUT, LockOptions.NO_WAIT )
										);
										fail( "Expecting a failure" );
									}
									catch ( LockTimeoutException | PessimisticLockException expected ) {
										// expected outcome
									}
								}
						);
					}
			);
		}
		finally {
			inTransaction(
					session -> session.createQuery( "delete Item" ).executeUpdate()
			);
		}
	}
}
