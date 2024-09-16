/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.locking;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
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
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	public void testNoWait()
			throws NoSuchFieldException, IllegalAccessException {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery(
				"select a from A a", A.class )
			.unwrap( Query.class )
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
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSkipLocked()
			throws NoSuchFieldException, IllegalAccessException {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery(
				"select a from A a", A.class )
			.unwrap( Query.class )
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
