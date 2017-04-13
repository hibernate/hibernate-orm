package org.hibernate.test.locking;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.SQLServer2005Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.SQLStatementInterceptor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PessimisticWriteLockTimeoutTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { A.class };
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	private A entity;

	@Before
	public void createTestData() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			entity = new A();
			session.persist( entity );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	@Test
	@RequiresDialect({ Oracle8iDialect.class, PostgreSQL81Dialect.class,
			SQLServer2005Dialect.class } )
	public void testNoWait()
			throws NoSuchFieldException, IllegalAccessException {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery(
				"select a from A a", A.class )
			.unwrap( org.hibernate.query.Query.class )
			.setLockOptions(
				new LockOptions( LockMode.PESSIMISTIC_WRITE )
		 	.setTimeOut( LockOptions.NO_WAIT ) )
			.list();

			String lockingQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( lockingQuery.toLowerCase().contains( "nowait") );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	@Test
	@RequiresDialect({ Oracle8iDialect.class, PostgreSQL95Dialect.class })
	public void testSkipLocked()
			throws NoSuchFieldException, IllegalAccessException {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery(
				"select a from A a", A.class )
			.unwrap( org.hibernate.query.Query.class )
			.setLockOptions(
				new LockOptions( LockMode.PESSIMISTIC_WRITE )
		 	.setTimeOut( LockOptions.SKIP_LOCKED ) )
			.list();

			String lockingQuery = sqlStatementInterceptor.getSqlQueries().getLast();
			assertTrue( lockingQuery.toLowerCase().contains( "skip locked") );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}
}
