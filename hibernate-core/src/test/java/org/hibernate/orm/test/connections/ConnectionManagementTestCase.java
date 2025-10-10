/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common test cases relating to session management and how the sessions
 * manages its underlying jdbc connection across different config
 * scenarios.  The different config scenarios are controlled by the
 * individual test subclasses.
 * <p>
 * In general, all the tests required are defined here in templated fashion.
 * Subclassed then override various hook methods specific to their given
 * scneario being tested.
 *
 * @author Steve Ebersole
 */

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/connections/Silly.hbm.xml"
)
@SessionFactory
public abstract class ConnectionManagementTestCase {


	// hooks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Used to prepare the environment for testing (e.g., starting a
	 * JTA transaction or obtaining a user-supplied connection).
	 *
	 * @throws Throwable indicates problems preparing
	 */
	protected void prepare() throws Throwable {
	}

	/**
	 * Used to cleanup the environment after testing (e.g., ending a JTA
	 * transaction or closing a user-supplied connection).
	 *
	 * @throws Throwable indicates problems cleaning up
	 */
	protected void done() throws Throwable {
	}

	/**
	 * Used to get a session configured based on the config scenario being
	 * tested.
	 *
	 * @return The session to be used in testing.
	 * @throws Throwable Indicates problems building a test session fixture.
	 */
	protected abstract Session getSessionUnderTest(SessionFactoryScope scope) throws Throwable;

	/**
	 * Used to release a {@link #getSessionUnderTest fixture session}.
	 * Overridden to perform session releasing/testing specific to the given
	 * config scenario being tested.
	 *
	 * @param session The session to be released.
	 */
	protected void release(Session session, SessionFactoryScope scope) {
		if ( session != null && session.isOpen() ) {
			try {
				session.close();
			}
			catch (Throwable ignore) {
			}
		}
	}

	protected void disconnect(Session session) {
		((SessionImplementor) session).getJdbcCoordinator().getLogicalConnection().manualDisconnect();
	}

	/**
	 * Perform any steps needed to reconnect a fixture session.
	 *
	 * @param session The fixture session to be reconnected.
	 * @throws Throwable Indicates problems reconnecting.
	 */
	protected abstract void reconnect(Session session) throws Throwable;

	/**
	 * Check the state of a fixture session after serialization, as well
	 * as validate the environmental state after session serialization.
	 *
	 * @param session The fixture session that was serialized.
	 */
	protected void checkSerializedState(Session session) {
	}

	/**
	 * Check the state of a fixture session after deserialization, as well
	 * as validate the environmental state after session deserialization.
	 *
	 * @param session The fixture session that was deserialized.
	 */
	protected void checkDeserializedState(Session session) {
	}


	// tests ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Tests to validate that a session holding JDBC resources will not
	 * be allowed to serialize.
	 */
	@Test
	public final void testConnectedSerialization(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session sessionUnderTest = getSessionUnderTest( scope );

		// force the connection to be retained
		try (ScrollableResults<Silly> sr = sessionUnderTest.createQuery( "from Silly", Silly.class ).scroll()) {
			sr.next();

			try {
				SerializationHelper.serialize( sessionUnderTest );

				fail( "Serialization of connected session allowed!" );
			}
			catch (IllegalStateException e) {
				// expected behaviour
			}
			finally {
				release( sessionUnderTest, scope );
				done();
			}
		}
	}

	/**
	 * Tests to validate that a session holding JDBC resources will not
	 * be allowed to serialize.
	 */
	@Test
	public final void testEnabledFilterSerialization(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session sessionUnderTest = null;
		Session s2 = null;
		try {
			sessionUnderTest = getSessionUnderTest( scope );

			sessionUnderTest.enableFilter( "nameIsNull" );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();
			disconnect( sessionUnderTest );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();

			byte[] bytes = SerializationHelper.serialize( sessionUnderTest );
			checkSerializedState( sessionUnderTest );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();
			reconnect( sessionUnderTest );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();
			disconnect( sessionUnderTest );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();

			s2 = (Session) SerializationHelper.deserialize( bytes );
			checkDeserializedState( s2 );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();
			reconnect( s2 );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();

			disconnect( s2 );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();
			reconnect( s2 );
			assertThat( sessionUnderTest.getEnabledFilter( "nameIsNull" ) ).isNotNull();

		}
		finally {
			release( sessionUnderTest, scope );
			release( s2, scope );
			done();
		}
	}

	/**
	 * Test that a session which has been manually disconnected will be allowed
	 * to serialize.
	 */
	@Test
	public final void testManualDisconnectedSerialization(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session sessionUnderTest = null;
		try {
			sessionUnderTest = getSessionUnderTest( scope );

			disconnect( sessionUnderTest );

			SerializationHelper.serialize( sessionUnderTest );
			checkSerializedState( sessionUnderTest );
		}
		finally {
			release( sessionUnderTest, scope );
			done();
		}
	}

	/**
	 * Test that the legacy manual disconnect()/reconnect() chain works as
	 * expected in the given environment.
	 */
	@Test
	public final void testManualDisconnectChain(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session sessionUnderTest = null;
		Session s2 = null;
		try {
			sessionUnderTest = getSessionUnderTest( scope );

			disconnect( sessionUnderTest );

			byte[] bytes = SerializationHelper.serialize( sessionUnderTest );
			checkSerializedState( sessionUnderTest );
			s2 = (Session) SerializationHelper.deserialize( bytes );
			checkDeserializedState( s2 );

			reconnect( s2 );

			disconnect( s2 );
			reconnect( s2 );
		}
		finally {
			release( sessionUnderTest, scope );
			release( s2, scope );
			done();
		}

	}

	/**
	 * Test that the legacy manual disconnect()/reconnect() chain works as
	 * expected in the given environment.  Similar to {@link #testManualDisconnectChain}
	 * expect that here we force the session to acquire and hold JDBC resources
	 * prior to disconnecting.
	 */
	@Test
	public final void testManualDisconnectWithOpenResources(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session sessionUnderTest = null;
		try {
			sessionUnderTest = getSessionUnderTest( scope );

			Silly silly = new Silly( "tester" );
			sessionUnderTest.persist( silly );
			sessionUnderTest.flush();

			try (ScrollableResults<Silly> sr = sessionUnderTest.createQuery( "from Silly", Silly.class ).scroll()) {

				disconnect( sessionUnderTest );
				SerializationHelper.serialize( sessionUnderTest );
				checkSerializedState( sessionUnderTest );

				reconnect( sessionUnderTest );
				sessionUnderTest.remove( silly );
				sessionUnderTest.flush();

			}
		}
		finally {
			release( sessionUnderTest, scope );
			done();
		}
	}

	/**
	 * Test that the basic session usage template works in all environment
	 * scenarios.
	 */
	@Test
	public void testBasicSessionUsage(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session s = null;
		Transaction txn = null;
		try {
			s = getSessionUnderTest( scope );
			txn = s.beginTransaction();
			s.createQuery( "from Silly", Silly.class ).list();
			txn.commit();
		}
		catch (Throwable t) {
			if ( txn != null ) {
				try {
					txn.rollback();
				}
				catch (Throwable ignore) {
				}
			}
		}
		finally {
			if ( s != null && s.isOpen() ) {
				try {
					s.close();
				}
				catch (Throwable ignore) {
				}
			}
		}
		done();
	}

	/**
	 * Test that session-closed protections work properly in all environments.
	 */
	@Test
	public void testSessionClosedProtections(SessionFactoryScope scope) throws Throwable {
		prepare();
		Session s = getSessionUnderTest( scope );
		release( s, scope );
		done();
		assertThat( s.isOpen() ).isFalse();
		assertThat( s.isConnected() ).isFalse();
		assertThat( s.getStatistics() ).isNotNull();
		assertThat( s.toString() ).isNotNull();

		try {
			s.createQuery( "from Silly" ).list();
			fail( "allowed to create query on closed session" );
		}
		catch (AssertionError testFailed) {
			throw testFailed;
		}
		catch (Throwable ignore) {
		}

		// you should be able to access the transaction on a closed EM. that is a change from what we used to do. we changed it
		// to better align with JPA.
		Transaction tran = s.getTransaction();
		assertThat( tran ).isNotNull();

		// Session implements both AutoCloseable and Closeable
		// Closable requires an idempotent behaviour, a closed resource must not throw an Exception
		// when close is called twice.
		s.close();

		try {
			s.isDirty();
			fail( "allowed to check dirtiness of closed session" );
		}
		catch (AssertionError testFailed) {
			throw testFailed;
		}
		catch (Throwable ignore) {
		}
	}
}
