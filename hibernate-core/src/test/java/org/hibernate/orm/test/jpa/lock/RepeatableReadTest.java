/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.math.BigDecimal;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.hibernate.orm.test.jpa.model.Part;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.jdbc.SQLServerSnapshotIsolationConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Test that the Hibernate Session complies with REPEATABLE_READ isolation
 * semantics.
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.DoesReadCommittedCauseWritersToBlockReadersCheck.class, reverse = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class RepeatableReadTest extends AbstractJPATest {

	private final SQLServerSnapshotIsolationConnectionProvider connectionProvider = new SQLServerSnapshotIsolationConnectionProvider();

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
	public void testStaleVersionedInstanceFoundInQueryResult() {
		String check = "EJB3 Specification";
		Item it = new Item( check );
		inTransaction(
				session -> session.persist( it )
		);

		Long itemId = it.getId();
		long initialVersion = it.getVersion();

		// Now, open a new Session and re-load the item...
		inTransaction(
				s1 -> {
					Item item = s1.find( Item.class, itemId );

					// now that the item is associated with the persistence-context of that session,
					// open a new session and modify it "behind the back" of the first session
					inTransaction(
							s2 -> {
								Item item2 = s2.find( Item.class, itemId );
								item2.setName( "EJB3 Persistence Spec" );
							}
					);

					// at this point, s1 now contains stale data, so try an hql query which
					// returns said item and make sure we get the previously associated state
					// (i.e., the old name and the old version)
					Item item2 = (Item) s1.createQuery( "select i from Item i" ).list().get( 0 );
					assertSame( item, item2 );
					assertEquals( check, item2.getName(), "encountered non-repeatable read" );
					assertEquals( initialVersion, item2.getVersion(), "encountered non-repeatable read" );

				}
		);

		// clean up
		inTransaction(
				session ->
						session.createQuery( "delete Item" ).executeUpdate()
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and fails to acquire a write lock after a TX in between committed changes to a row")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	public void testStaleVersionedInstanceFoundOnLock() {
		if ( !readCommittedIsolationMaintained( "repeatable read tests" ) ) {
			return;
		}
		String check = "EJB3 Specification";
		Item it = new Item( check );
		inTransaction(
				session -> session.persist( it )
		);

		Long itemId = it.getId();
		long initialVersion = it.getVersion();

		// Now, open a new Session and re-load the item...
		inSession(
				s1 -> {
					s1.beginTransaction();
					try {
						Item item = s1.find( Item.class, itemId );

						// now that the item is associated with the persistence-context of that session,
						// open a new session and modify it "behind the back" of the first session
						inTransaction(
								s2 -> {
									Item item2 = s2.find( Item.class, itemId );
									item2.setName( "EJB3 Persistence Spec" );
								}
						);

						// at this point, s1 now contains stale data, so acquire a READ lock
						// and make sure we get the already associated state (i.e., the old
						// name and the old version)
						s1.lock( item, LockMode.READ );
						Item item2 = (Item) s1.find( Item.class, itemId );
						assertSame( item, item2 );
						assertEquals( check, item2.getName(), "encountered non-repeatable read" );
						assertEquals( initialVersion, item2.getVersion(), "encountered non-repeatable read" );

						// attempt to acquire an UPGRADE lock; this should fail

						s1.lock( item, LockMode.PESSIMISTIC_WRITE );
						fail( "expected UPGRADE lock failure" );
					}
					catch (OptimisticLockException expected) {
						assertInstanceOf( StaleObjectStateException.class, expected.getCause() );
						// this is the expected behavior
					}
					catch (SQLGrammarException t) {
						if ( getDialect() instanceof SQLServerDialect ) {
							// sql-server (using snapshot isolation) reports this as a grammar exception /:)
							//
							// not to mention that it seems to "lose track" of the transaction in this scenario...
							s1.getTransaction().rollback();
						}
						else {
							throw t;
						}
					}
					finally {
						s1.getTransaction().rollback();
					}

				}
		);

		// clean up
		inTransaction(
				session ->
						session.createQuery( "delete Item" ).executeUpdate()
		);
	}

	@Test
	public void testStaleNonVersionedInstanceFoundInQueryResult() {
		String check = "Lock Modes";
		Part p = new Part( new Item( "EJB3 Specification" ), check, "3.3.5.3", new BigDecimal( "0.0" ) );
		inTransaction(
				session -> session.persist( p )
		);

		Long partId = p.getId();

		// Now, open a new Session and re-load the part...

		inTransaction(
				s1 -> {
					Part part = s1.find( Part.class, partId );

					// now that the item is associated with the persistence-context of that session,
					// open a new session and modify it "behind the back" of the first session
					inTransaction(
							s2 -> {
								Part part2 = s2.find( Part.class, partId );
								part2.setName( "Lock Mode Types" );
							}
					);

					// at this point, s1 now contains stale data, so try an hql query which
					// returns said part and make sure we get the previously associated state
					// (i.e., the old name)
					Part part2 = (Part) s1.createQuery( "select p from Part p" ).list().get( 0 );
					assertSame( part, part2 );
					assertEquals( check, part2.getName(), "encountered non-repeatable read" );
				}
		);

		// clean up
		inTransaction(
				session -> {
					Part part = (Part) session.createQuery( "select p from Part p" ).list().get( 0 );

					session.remove( part );
					session.remove( part.getItem() );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach uses SERIALIZABLE by default and fails to acquire a write lock after a TX in between committed changes to a row")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB will throw an error DB_RECORD_CHANGED when acquiring a lock on a record that have changed")
	public void testStaleNonVersionedInstanceFoundOnLock() {
		if ( !readCommittedIsolationMaintained( "repeatable read tests" ) ) {
			return;
		}
		String check = "Lock Modes";
		Part p = new Part( new Item( "EJB3 Specification" ), check, "3.3.5.3", new BigDecimal( "0.0" ) );
		inTransaction(
				session -> session.persist( p )
		);

		Long partId = p.getId();

		// Now, open a new Session and re-load the part...
		inTransaction(
				s1 -> {
					Part part = s1.find( Part.class, partId );

					// now that the item is associated with the persistence-context of that session,
					// open a new session and modify it "behind the back" of the first session
					inTransaction(
							s2 -> {
								Part part2 = s2.find( Part.class, partId );
								part2.setName( "Lock Mode Types" );
							}
					);

					// at this point, s1 now contains stale data, so acquire a READ lock
					// and make sure we get the already associated state (i.e., the old
					// name and the old version)
					s1.lock( part, LockMode.READ );
					Part part2 = s1.find( Part.class, partId );
					assertSame( part, part2 );
					assertEquals( check, part2.getName(), "encountered non-repeatable read" );

					// then acquire an UPGRADE lock; this should fail
					try {
						s1.lock( part, LockMode.PESSIMISTIC_WRITE );
					}
					catch (Throwable t) {
						// SQLServer, for example, immediately throws an exception here...
						s1.getTransaction().rollback();
						s1.beginTransaction();
					}
					part2 = s1.find( Part.class, partId );
					assertSame( part, part2 );
					assertEquals( check, part2.getName(), "encountered non-repeatable read" );
				}
		);

		// clean up
		inTransaction(
				session -> {
					Part part = session.find( Part.class, partId );
					session.remove( part );
					session.remove( part.getItem() );
				}
		);
	}
}
