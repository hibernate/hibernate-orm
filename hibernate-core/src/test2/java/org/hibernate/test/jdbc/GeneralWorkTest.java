/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * GeneralWorkTest implementation
 *
 * @author Steve Ebersole
 */
public class GeneralWorkTest extends BaseCoreFunctionalTestCase {
	@Override
	public String getBaseForMappings() {
		return "org/hibernate/test/jdbc/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "Mappings.hbm.xml" };
	}

	@Test
	public void testGeneralUsage() throws Throwable {
		final Session session = openSession();
		session.beginTransaction();
		session.doWork(
				new Work() {
					public void execute(Connection connection) throws SQLException {
						// in this current form, users must handle try/catches themselves for proper resource release
						Statement statement = null;
						try {
							statement = ((SessionImplementor)session).getJdbcCoordinator().getStatementPreparer().createStatement();
							ResultSet resultSet = null;
							try {
								
								resultSet = ((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from T_JDBC_PERSON" );
							}
							finally {
								releaseQuietly( ((SessionImplementor)session), resultSet, statement );
							}
							try {
								((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from T_JDBC_BOAT" );
							}
							finally {
								releaseQuietly( ((SessionImplementor)session), resultSet, statement );
							}
						}
						finally {
							releaseQuietly( ((SessionImplementor)session), statement );
						}
					}
				}
		);
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSQLExceptionThrowing() {
		final Session session = openSession();
		session.beginTransaction();
		try {
			session.doWork(
					new Work() {
						public void execute(Connection connection) throws SQLException {
							Statement statement = null;
							try {
								statement = ((SessionImplementor)session).getJdbcCoordinator().getStatementPreparer().createStatement();
								((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from non_existent" );
							}
							finally {
								releaseQuietly( ((SessionImplementor)session), statement );
							}
						}
					}
			);
			fail( "expecting exception" );
		}
		catch ( JDBCException expected ) {
			// expected outcome
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testGeneralReturningUsage() throws Throwable {
		Session session = openSession();
		session.beginTransaction();
		Person p = new Person( "Abe", "Lincoln" );
		session.save( p );
		session.getTransaction().commit();

		final Session session2 = openSession();
		session2.beginTransaction();
		long count = session2.doReturningWork(
				new ReturningWork<Long>() {
					public Long execute(Connection connection) throws SQLException {
						// in this current form, users must handle try/catches themselves for proper resource release
						Statement statement = null;
						long personCount = 0;
						try {
							statement = ((SessionImplementor)session2).getJdbcCoordinator().getStatementPreparer().createStatement();
							ResultSet resultSet = null;
							try {
								resultSet = ((SessionImplementor)session2).getJdbcCoordinator().getResultSetReturn().extract( statement, "select count(*) from T_JDBC_PERSON" );
								resultSet.next();
								personCount = resultSet.getLong( 1 );
								assertEquals( 1L, personCount );
							}
							finally {
								releaseQuietly( ((SessionImplementor)session2), resultSet, statement );
							}
						}
						finally {
							releaseQuietly( ((SessionImplementor)session2), statement );
						}
						return personCount;
					}
				}
		);
		session2.getTransaction().commit();
		session2.close();
		assertEquals( 1L, count );

		session = openSession();
		session.beginTransaction();
		session.delete( p );
		session.getTransaction().commit();
		session.close();
	}

	private void releaseQuietly(SessionImplementor s, Statement statement) {
		if ( statement == null ) {
			return;
		}
		try {
			s.getJdbcCoordinator().getResourceRegistry().release( statement );
		}
		catch (Exception e) {
			// ignore
		}
	}

	private void releaseQuietly(SessionImplementor s, ResultSet resultSet, Statement statement) {
		if ( resultSet == null ) {
			return;
		}
		try {
			s.getJdbcCoordinator().getResourceRegistry().release( resultSet, statement );
		}
		catch (Exception e) {
			// ignore
		}
	}
}
