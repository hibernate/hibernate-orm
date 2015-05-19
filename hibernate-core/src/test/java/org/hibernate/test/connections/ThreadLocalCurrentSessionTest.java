/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.connections;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class ThreadLocalCurrentSessionTest extends ConnectionManagementTestCase {
	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( Environment.CURRENT_SESSION_CONTEXT_CLASS, TestableThreadLocalContext.class.getName() );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Session getSessionUnderTest() throws Throwable {
		Session session = sessionFactory().getCurrentSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void release(Session session) {
		if ( session.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			TestableThreadLocalContext.unbind( sessionFactory() );
			return;
		}
		long initialCount = sessionFactory().getStatistics().getSessionCloseCount();
		session.getTransaction().commit();
		long subsequentCount = sessionFactory().getStatistics().getSessionCloseCount();
		assertEquals( "Session still open after commit", initialCount + 1, subsequentCount );
		// also make sure it was cleaned up from the internal ThreadLocal...
		assertFalse( "session still bound to internal ThreadLocal", TestableThreadLocalContext.hasBind() );
	}

	@Override
	protected void reconnect(Session session) throws Throwable {
	}

	@Override
	protected void checkSerializedState(Session session) {
		assertFalse( "session still bound after serialize", TestableThreadLocalContext.isSessionBound( session ) );
	}

	@Override
	protected void checkDeserializedState(Session session) {
		assertTrue( "session not bound after deserialize", TestableThreadLocalContext.isSessionBound( session ) );
	}

	@Test
	public void testTransactionProtection() {
		Session session = sessionFactory().getCurrentSession();
		try {
			session.createQuery( "from Silly" );
			fail( "method other than beginTransaction{} allowed" );
		}
		catch ( HibernateException e ) {
			// ok
		}
	}

	@Test
	public void testContextCleanup() {
		Session session = sessionFactory().getCurrentSession();
		session.beginTransaction();
		session.getTransaction().commit();
		assertFalse( "session open after txn completion", session.isOpen() );
		assertFalse( "session still bound after txn completion", TestableThreadLocalContext.isSessionBound( session ) );

		Session session2 = sessionFactory().getCurrentSession();
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
			return sessionMap() != null && sessionMap().containsKey( me.factory() )
					&& sessionMap().get( me.factory() ) == session;
		}

		public static boolean hasBind() {
			return sessionMap() != null && sessionMap().containsKey( me.factory() );
		}
	}
}
