package org.hibernate.test.connections;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.context.ThreadLocalSessionContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Steve Ebersole
 */
public class ThreadLocalCurrentSessionTest extends ConnectionManagementTestCase {

	public ThreadLocalCurrentSessionTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ThreadLocalCurrentSessionTest.class );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.CURRENT_SESSION_CONTEXT_CLASS, TestableThreadLocalContext.class.getName() );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	protected Session getSessionUnderTest() throws Throwable {
		Session session = getSessions().getCurrentSession();
		session.beginTransaction();
		return session;
	}

	protected void release(Session session) {
		long initialCount = getSessions().getStatistics().getSessionCloseCount();
		session.getTransaction().commit();
		long subsequentCount = getSessions().getStatistics().getSessionCloseCount();
		assertEquals( "Session still open after commit", initialCount + 1, subsequentCount );
		// also make sure it was cleaned up from the internal ThreadLocal...
		assertFalse( "session still bound to internal ThreadLocal", TestableThreadLocalContext.hasBind() );
	}

	protected void reconnect(Session session) throws Throwable {
//		session.reconnect();
		session.beginTransaction();
	}

	protected void checkSerializedState(Session session) {
		assertFalse( "session still bound after serialize", TestableThreadLocalContext.isSessionBound( session ) );
	}

	protected void checkDeserializedState(Session session) {
		assertTrue( "session not bound after deserialize", TestableThreadLocalContext.isSessionBound( session ) );
	}

	public void testTransactionProtection() {
		Session session = getSessions().getCurrentSession();
		try {
			session.createQuery( "from Silly" );
			fail( "method other than beginTransaction{} allowed" );
		}
		catch ( HibernateException e ) {
			// ok
		}
	}

	public void testContextCleanup() {
		Session session = getSessions().getCurrentSession();
		session.beginTransaction();
		session.getTransaction().commit();
		assertFalse( "session open after txn completion", session.isOpen() );
		assertFalse( "session still bound after txn completion", TestableThreadLocalContext.isSessionBound( session ) );

		Session session2 = getSessions().getCurrentSession();
		assertFalse( "same session returned after txn completion", session == session2 );
		session2.close();
		assertFalse( "session open after closing", session2.isOpen() );
		assertFalse( "session still bound after closing", TestableThreadLocalContext.isSessionBound( session2 ) );
	}

	public static class TestableThreadLocalContext extends ThreadLocalSessionContext {
		private static TestableThreadLocalContext me;

		public TestableThreadLocalContext(SessionFactoryImplementor factory) {
			super( factory );
			me = this;
		}

		public static boolean isSessionBound(Session session) {
			return sessionMap() != null && sessionMap().containsKey( me.factory )
					&& sessionMap().get( me.factory ) == session;
		}

		public static boolean hasBind() {
			return sessionMap() != null && sessionMap().containsKey( me.factory );
		}
	}
}
