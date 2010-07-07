// $Id: AggressiveReleaseTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.connections;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.impl.SessionImpl;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.testing.tm.ConnectionProviderImpl;
import org.hibernate.testing.tm.SimpleJtaTransactionManagerImpl;
import org.hibernate.testing.tm.TransactionManagerLookupImpl;
import org.hibernate.transaction.CMTTransactionFactory;
import org.hibernate.util.SerializationHelper;

/**
 * Implementation of AggressiveReleaseTest.
 *
 * @author Steve Ebersole
 */
public class AggressiveReleaseTest extends ConnectionManagementTestCase {

	public AggressiveReleaseTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( AggressiveReleaseTest.class );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, ConnectionProviderImpl.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, TransactionManagerLookupImpl.class.getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.AFTER_STATEMENT.toString() );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}

	protected Session getSessionUnderTest() throws Throwable {
		return openSession();
	}

	protected void reconnect(Session session) {
		session.reconnect();
	}

	protected void prepare() throws Throwable {
		SimpleJtaTransactionManagerImpl.getInstance().begin();
	}

	protected void done() throws Throwable {
		SimpleJtaTransactionManagerImpl.getInstance().commit();
	}

	// Some additional tests specifically for the aggressive-release functionality...

	public void testSerializationOnAfterStatementAggressiveRelease() throws Throwable {
		prepare();
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
		done();
	}

	public void testSerializationFailsOnAfterStatementAggressiveReleaseWithOpenResources() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();

		Silly silly = new Silly( "silly" );
		s.save( silly );

		// this should cause the CM to obtain a connection, and then release it
		s.flush();

		// both scroll() and iterate() cause the batcher to hold on
		// to resources, which should make aggresive-release not release
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

		// Closing the ScrollableResults does currently force the batcher to
		// aggressively release the connection
		sr.close();
		SerializationHelper.serialize( s );

		s.delete( silly );
		s.flush();

		release( s );
		done();
	}

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

	public void testSuppliedConnection() throws Throwable {
		prepare();

		Connection originalConnection = ConnectionProviderImpl.getActualConnectionProvider().getConnection();
		Session session = getSessions().openSession( originalConnection );

		Silly silly = new Silly( "silly" );
		session.save( silly );

		// this will cause the connection manager to cycle through the aggressive release logic;
		// it should not release the connection since we explicitly suplied it ourselves.
		session.flush();

		assertTrue( "Different connections", originalConnection == session.connection() );

		session.delete( silly );
		session.flush();

		release( session );
		done();

		ConnectionProviderImpl.getActualConnectionProvider().closeConnection( originalConnection );
	}

	public void testBorrowedConnections() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();

		Connection conn = s.connection();
		assertTrue( ( ( SessionImpl ) s ).getJDBCContext().getConnectionManager().hasBorrowedConnection() );
		conn.close();
		assertFalse( ( ( SessionImpl ) s ).getJDBCContext().getConnectionManager().hasBorrowedConnection() );

		release( s );
		done();
	}

	public void testConnectionMaintanenceDuringFlush() throws Throwable {
		prepare();
		Session s = getSessionUnderTest();
		s.beginTransaction();

		List entities = new ArrayList();
		for ( int i = 0; i < 10; i++ ) {
			Other other = new Other( "other-" + i );
			Silly silly = new Silly( "silly-" + i, other );
			entities.add( silly );
			s.save( silly );
		}
		s.flush();

		Iterator itr = entities.iterator();
		while ( itr.hasNext() ) {
			Silly silly = ( Silly ) itr.next();
			silly.setName( "new-" + silly.getName() );
			silly.getOther().setName( "new-" + silly.getOther().getName() );
		}
		long initialCount = getSessions().getStatistics().getConnectCount();
		s.flush();
		assertEquals( "connection not maintained through flush", initialCount + 1, getSessions().getStatistics().getConnectCount() );

		s.createQuery( "delete from Silly" ).executeUpdate();
		s.createQuery( "delete from Other" ).executeUpdate();
		s.getTransaction().commit();
		release( s );
		done();
	}
}
