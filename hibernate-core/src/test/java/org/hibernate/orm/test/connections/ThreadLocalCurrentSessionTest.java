/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = Environment.CURRENT_SESSION_CONTEXT_CLASS,
				provider = ThreadLocalCurrentSessionTest.CurrentSessionContextClassProvider.class

		)
)
@SessionFactory(
		generateStatistics = true
)
public class ThreadLocalCurrentSessionTest extends ConnectionManagementTestCase {

	public static class CurrentSessionContextClassProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return TestableThreadLocalContext.class.getName();
		}
	}


	@Override
	protected Session getSessionUnderTest(SessionFactoryScope scope) {
		Session session = scope.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void release(Session session, SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		if ( session.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			TestableThreadLocalContext.unbind( sessionFactory );
			return;
		}
		long initialCount = sessionFactory.getStatistics().getSessionCloseCount();
		session.getTransaction().commit();
		long subsequentCount = sessionFactory.getStatistics().getSessionCloseCount();
		assertThat( subsequentCount )
				.describedAs( "Session still open after commit" )
				.isEqualTo( initialCount + 1 );
		// also make sure it was cleaned up from the internal ThreadLocal...
		assertThat( TestableThreadLocalContext.hasBind() )
				.describedAs( "session still bound to internal ThreadLocal" )
				.isFalse();
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	protected void checkSerializedState(Session session) {
		assertThat( TestableThreadLocalContext.isSessionBound( session ) )
				.describedAs( "session still bound after serialize" )
				.isFalse();
	}

	@Override
	protected void checkDeserializedState(Session session) {
		assertThat( TestableThreadLocalContext.isSessionBound( session ) )
				.describedAs( "session not bound after deserialize" )
				.isTrue();
	}

	@Test
	@JiraKey(value = "HHH-11067")
	public void testEqualityChecking(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		Session session1 = null;
		Session session2 = null;
		try {
			session1 = sessionFactory.getCurrentSession();
			session2 = sessionFactory.getCurrentSession();
			assertThat( session1 ).isSameAs( session2 );
			assertThat( session1 ).isEqualTo( session2 );
		}
		finally {
			release( session1, scope );
			release( session2, scope );
		}
	}

	@Test
	public void testTransactionProtection(SessionFactoryScope scope) {
		try (Session session = scope.getSessionFactory().getCurrentSession()) {

			session.createQuery( "from Silly" );
			fail( "method other than beginTransaction() allowed" );
		}
		catch (HibernateException e) {
			// ok
		}
	}

	@Test
	public void testContextCleanup(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		Session session = null;
		Session session2 = null;
		try {
			session = sessionFactory.getCurrentSession();
			session.beginTransaction();
			session.getTransaction().commit();
			assertThat( session.isOpen() )
					.describedAs( "session open after txn completion" )
					.isFalse();
			assertThat( TestableThreadLocalContext.isSessionBound( session ) )
					.describedAs( "session still bound after txn completion" )
					.isFalse();

			session2 = sessionFactory.getCurrentSession();
			assertThat( session == session2 )
					.describedAs( "same session returned after txn completion" )
					.isFalse();
			session2.close();
			assertThat( session2.isOpen() )
					.describedAs( "session open after closing" )
					.isFalse();
			assertThat( TestableThreadLocalContext.isSessionBound( session2 ) )
					.describedAs( "session still bound after closing" )
					.isFalse();
		}
		finally {
			release( session, scope );
			release( session2, scope );
		}
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
