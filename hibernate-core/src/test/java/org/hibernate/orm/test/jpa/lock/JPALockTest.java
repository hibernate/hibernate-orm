/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.jdbc.SQLServerSnapshotIsolationConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests specifically relating to section 3.3.5.3 [Lock Modes] of the
 * JPA persistence specification (as of the <i>Proposed Final Draft</i>).
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.DoesReadCommittedCauseWritersToBlockReadersCheck.class, reverse = true)
public class JPALockTest extends AbstractJPATest {

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

	/**
	 * Test the equivalent of EJB3 LockModeType.READ
	 * <p>
	 * From the spec:
	 * <p>
	 * If transaction T1 calls lock(entity, LockModeType.READ) on a versioned object, the entity
	 * manager must ensure that neither of the following phenomena can occur:<ul>
	 * <li>P1 (Dirty read): Transaction T1 modifies a row. Another transaction T2 then reads that row and
	 * obtains the modified value, before T1 has committed or rolled back. Transaction T2 eventually
	 * commits successfully; it does not matter whether T1 commits or rolls back and whether it does
	 * so before or after T2 commits.
	 * <li>P2 (Non-repeatable read): Transaction T1 reads a row. Another transaction T2 then modifies or
	 * deletes that row, before T1 has committed. Both transactions eventually commit successfully.
	 * <p>
	 * This will generally be achieved by the entity manager acquiring a lock on the underlying database row.
	 * Any such lock may be obtained immediately (so long as it is retained until commit completes), or the
	 * lock may be deferred until commit time (although even then it must be retained until the commit completes).
	 * Any implementation that supports repeatable reads in a way that prevents the above phenomena
	 * is permissible.
	 * <p>
	 * The persistence implementation is not required to support calling lock(entity, LockMode-Type.READ)
	 * on a non-versioned object. When it cannot support such a lock call, it must throw the
	 * PersistenceException. When supported, whether for versioned or non-versioned objects, LockMode-Type.READ
	 * must always prevent the phenomena P1 and P2. Applications that call lock(entity, LockModeType.READ)
	 * on non-versioned objects will not be portable.
	 * <p>
	 * EJB3 LockModeType.READ actually maps to the Hibernate LockMode.OPTIMISTIC
	 */
	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and fails to acquire a write lock after a TX in between committed changes to a row")
	public void testLockModeTypeRead() {
		if ( !readCommittedIsolationMaintained( "ejb3 lock tests" ) ) {
			return;
		}
		final String initialName = "lock test";
		// set up some test data
		Item it = new Item();
		Long itemId = fromTransaction(
				session -> {
					it.setName( initialName );
					session.persist( it );
					return it.getId();
				}
		);

		Session s1 = null;
		Session s2 = null;
		Item item;
		try {
			// do the isolated update
			s1 = sessionFactory().openSession();
			s1.beginTransaction();
			item = s1.get( Item.class, itemId );
			s1.lock( item, LockMode.PESSIMISTIC_WRITE );
			item.setName( "updated" );
			s1.flush();

			s2 = sessionFactory().openSession();
			Transaction t2 = s2.beginTransaction();
			Item item2 = s2.get( Item.class, itemId );
			assertEquals( initialName, item2.getName(), "isolation not maintained" );

			s1.getTransaction().commit();
			s1.close();

			item2 = s2.get( Item.class, itemId );
			assertEquals( initialName, item2.getName(), "repeatable read not maintained" );
			t2.commit();
			s2.close();
		}
		finally {
			if ( s1 != null ) {
				try {
					if ( s1.getTransaction().isActive() ) {
						s1.getTransaction().rollback();
					}
				}
				finally {
					if ( s1.isOpen() ) {
						s1.close();
					}
				}
			}

			if ( s2 != null ) {
				try {
					if ( s2.getTransaction().isActive() ) {
						s2.getTransaction().rollback();
					}
				}
				finally {
					if ( s2.isOpen() ) {
						s2.close();
					}
				}
			}
		}

		inTransaction(
				session ->
						session.remove( session.getReference(item) )
		);
	}

	/**
	 * Test the equivalent of EJB3 LockModeType.WRITE
	 * <p>
	 * From the spec:
	 * <p>
	 * If transaction T1 calls lock(entity, LockModeType.WRITE) on a versioned object, the entity
	 * manager must avoid the phenomena P1 and P2 (as with LockModeType.READ) and must also force
	 * an update (increment) to the entity's version column. A forced version update may be performed immediately,
	 * or may be deferred until a flush or commit. If an entity is removed before a deferred version
	 * update was to have been applied, the forced version update is omitted, since the underlying database
	 * row no longer exists.
	 * <p>
	 * The persistence implementation is not required to support calling lock(entity, LockMode-Type.WRITE)
	 * on a non-versioned object. When it cannot support a such lock call, it must throw the
	 * PersistenceException. When supported, whether for versioned or non-versioned objects, LockMode-Type.WRITE
	 * must always prevent the phenomena P1 and P2. For non-versioned objects, whether or
	 * not LockModeType.WRITE has any additional behaviour is vendor-specific. Applications that call
	 * lock(entity, LockModeType.WRITE) on non-versioned objects will not be portable.
	 */
	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and fails to acquire a write lock after a TX in between committed changes to a row")
	public void testLockModeTypeWrite() {
		if ( !readCommittedIsolationMaintained( "ejb3 lock tests" ) ) {
			return;
		}
		final String initialName = "lock test";
		// set up some test data
		Item it = new Item();
		inTransaction(
				session -> {
					it.setName( initialName );
					session.persist( it );
				}
		);

		Long itemId = it.getId();
		long initialVersion = it.getVersion();
		Session s1 = null;
		Session s2 = null;
		Item item;
		try {
			s1 = sessionFactory().openSession();
			s1.beginTransaction();
			item = s1.get( Item.class, itemId );
			s1.lock( item, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertEquals( initialVersion + 1, item.getVersion(), "no forced version increment" );

			s1.lock( item, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertEquals( initialVersion + 1, item.getVersion(), "subsequent LockMode.FORCE did not no-op" );

			s2 = sessionFactory().openSession();
			s2.beginTransaction();
			Item item2 = s2.get( Item.class, itemId );
			assertEquals( initialName, item2.getName(), "isolation not maintained" );

			item.setName( "updated-1" );
			s1.flush();
			// currently an unfortunate side effect...
			assertEquals( initialVersion + 2, item.getVersion() );

			s1.getTransaction().commit();
			s1.close();

			item2.setName( "updated" );
			try {
				s2.getTransaction().commit();
				fail( "optimistic lock should have failed" );
			}
			catch (Throwable t) {
				// expected behavior
				try {
					s2.getTransaction().rollback();
				}
				catch (Throwable ignore) {
					// ignore
				}
				if ( t instanceof AssertionError ) {
					throw (AssertionError) t;
				}
			}
		}
		finally {
			if ( s1 != null ) {
				try {
					if ( s1.getTransaction().isActive() ) {
						s1.getTransaction().rollback();
					}
				}
				finally {
					if ( s1.isOpen() ) {
						s1.close();
					}
				}
			}

			if ( s2 != null ) {
				try {
					if ( s2.getTransaction().isActive() ) {
						s2.getTransaction().rollback();
					}
				}
				finally {
					if ( s2.isOpen() ) {
						s2.close();
					}
				}
			}
		}

		inTransaction(
				session -> {
					session.remove( session.getReference(item) );
				}
		);
	}
}
