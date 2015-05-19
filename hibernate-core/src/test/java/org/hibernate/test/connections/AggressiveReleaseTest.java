/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: AggressiveReleaseTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.connections;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Implementation of AggressiveReleaseTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class AggressiveReleaseTest extends ConnectionManagementTestCase {
	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
//		settings.put( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() );
		settings.put( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	@Override
	protected Session getSessionUnderTest() throws Throwable {
		return openSession();
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	protected void prepare() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
	}

	@Override
	protected void done() throws Throwable {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
	}

	// Some additional tests specifically for the aggressive-release functionality...

	@Test
	public void testSerializationOnAfterStatementAggressiveRelease() throws Throwable {
		prepare();
		try {
			Session s = getSessionUnderTest();
			Silly silly = new Silly( "silly" );
			s.save( silly );

			// this should cause the CM to obtain a connection, and then release it
			s.flush();

			// We should be able to serialize the session at this point...
			SerializationHelper.serialize( s );

			s.delete( silly );
			s.flush();

			release( s );
		}
		finally {
			done();
		}
	}

	@Test
	public void testSerializationFailsOnAfterStatementAggressiveReleaseWithOpenResources() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();

		Silly silly = new Silly( "silly" );
		s.save( silly );

		// this should cause the CM to obtain a connection, and then release it
		s.flush();

		// both scroll() and iterate() cause batching to hold on
		// to resources, which should make aggressive-release not release
		// the connection (and thus cause serialization to fail)
		ScrollableResults sr = s.createQuery( "from Silly" ).scroll();

		try {
			SerializationHelper.serialize( s );
			fail( "Serialization allowed on connected session; or aggressive release released connection with open resources" );
		}
		catch( IllegalStateException e ) {
			// expected behavior
		}

		// getting the first row only because SybaseASE15Dialect throws NullPointerException
		// if data is not read before closing the ResultSet
		sr.next();

		// Closing the ScrollableResults does currently force batching to
		// aggressively release the connection
		sr.close();
		SerializationHelper.serialize( s );

		s.delete( silly );
		s.flush();

		release( s );
		done();
	}

	@Test
	public void testQueryIteration() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();
		Silly silly = new Silly( "silly" );
		s.save( silly );
		s.flush();

		Iterator itr = s.createQuery( "from Silly" ).iterate();
		assertTrue( itr.hasNext() );
		Silly silly2 = ( Silly ) itr.next();
		assertEquals( silly, silly2 );
		Hibernate.close( itr );

		itr = s.createQuery( "from Silly" ).iterate();
		Iterator itr2 = s.createQuery( "from Silly where name = 'silly'" ).iterate();

		assertTrue( itr.hasNext() );
		assertEquals( silly, itr.next() );
		assertTrue( itr2.hasNext() );
		assertEquals( silly, itr2.next() );

		Hibernate.close( itr );
		Hibernate.close( itr2 );

		s.delete( silly );
		s.flush();

		release( s );
		done();
	}

	@Test
	public void testQueryScrolling() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();
		Silly silly = new Silly( "silly" );
		s.save( silly );
		s.flush();

		ScrollableResults sr = s.createQuery( "from Silly" ).scroll();
		assertTrue( sr.next() );
		Silly silly2 = ( Silly ) sr.get( 0 );
		assertEquals( silly, silly2 );
		sr.close();

		sr = s.createQuery( "from Silly" ).scroll();
		ScrollableResults sr2 = s.createQuery( "from Silly where name = 'silly'" ).scroll();

		assertTrue( sr.next() );
		assertEquals( silly, sr.get( 0 ) );
		assertTrue( sr2.next() );
		assertEquals( silly, sr2.get( 0 ) );

		sr.close();
		sr2.close();

		s.delete( silly );
		s.flush();

		release( s );
		done();
	}

	@Test
	public void testSuppliedConnection() throws Throwable {
		prepare();

		Connection originalConnection = sessionFactory().getServiceRegistry().getService( ConnectionProvider.class ).getConnection();
		Session session = sessionFactory().withOptions().connection( originalConnection ).openSession();

		Silly silly = new Silly( "silly" );
		session.save( silly );

		// this will cause the connection manager to cycle through the aggressive release logic;
		// it should not release the connection since we explicitly suplied it ourselves.
		session.flush();
		assertTrue( session.isConnected() );

		session.delete( silly );
		session.flush();

		release( session );
		done();

		sessionFactory().getServiceRegistry().getService( ConnectionProvider.class ).closeConnection( originalConnection );
	}

	@Test
	public void testConnectionMaintanenceDuringFlush() throws Throwable {
		final Statistics statistics = sessionFactory().getStatistics();
		prepare();
		Session s = getSessionUnderTest();

		List<Silly> entities = new ArrayList<Silly>();
		for ( int i = 0; i < 10; i++ ) {
			Other other = new Other( "other-" + i );
			Silly silly = new Silly( "silly-" + i, other );
			entities.add( silly );
			s.save( silly );
		}
		s.flush();

		for ( Silly silly : entities ) {
			silly.setName( "new-" + silly.getName() );
			silly.getOther().setName( "new-" + silly.getOther().getName() );
		}
		long initialCount = statistics.getConnectCount();
		s.flush();
		assertEquals( "connection not maintained through flush", initialCount + 1, statistics.getConnectCount() );

		s.createQuery( "delete from Silly" ).executeUpdate();
		s.createQuery( "delete from Other" ).executeUpdate();
		s.getTransaction().commit();
		release( s );
		done();
	}
}
