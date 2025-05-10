/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;
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
	public void testNoWait() {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery( "select a from A a", A.class )
					.unwrap( Query.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setTimeout( Timeouts.NO_WAIT )
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
	public void testSkipLocked() {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery("select a from A a", A.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setTimeout( Timeouts.SKIP_LOCKED )
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
