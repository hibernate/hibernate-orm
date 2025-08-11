/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.hibernate.Timeouts;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.hibernate.testing.jdbc.SQLServerSnapshotIsolationConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportNoWait.class)
public class LockExceptionTests extends AbstractJPATest {

	private SQLServerSnapshotIsolationConnectionProvider connectionProvider = new SQLServerSnapshotIsolationConnectionProvider();

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		super.applySettings( builder );
		if ( SQLServerDialect.class.isAssignableFrom( DIALECT.getClass() ) ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) builder.getSettings()
					.get( AvailableSettings.CONNECTION_PROVIDER ) );
			builder.applySetting( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		}
	}

	@AfterAll
	protected void tearDown() {
		connectionProvider.stop();
	}

	@Test
	@JiraKey( value = "HHH-8786" )
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "for update clause does not imply locking. See https://github.com/cockroachdb/cockroach/issues/88995")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "no failure")
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
												Timeouts.NO_WAIT
										);
										fail( "Expecting a failure" );
									}
									catch (LockTimeoutException | PessimisticLockException | LockAcquisitionException expected ) {
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
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading a row that is exclusively locked by a different TX")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Cursor must be on simple SELECT for FOR UPDATE")
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
												Timeouts.NO_WAIT
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
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading a row that is exclusively locked by a different TX")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "no failure")
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
										Item item2 = secondSession.find( Item.class, item.getId() );
										secondSession.lock(
												item2,
												LockModeType.PESSIMISTIC_WRITE,
												Timeouts.NO_WAIT
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
