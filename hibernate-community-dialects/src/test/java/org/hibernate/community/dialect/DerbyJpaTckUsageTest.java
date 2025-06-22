/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.orm.test.jpa.procedure.User;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests various JPA usage scenarios for performing stored procedures.  Inspired by the awesomely well-done JPA TCK
 *
 * @author Steve Ebersole
 */
@RequiresDialect(DerbyDialect.class)
public class DerbyJpaTckUsageTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ User.class};
	}

	@Test
	public void testMultipleGetUpdateCountCalls() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser" );
			// this is what the TCK attempts to do, don't shoot the messenger...
			query.getUpdateCount();
			// yep, twice
			query.getUpdateCount();
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	public void testBasicScalarResults() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser" );
			boolean isResult = query.execute();
			assertTrue( isResult );
			int updateCount = query.getUpdateCount();

			boolean results = false;
			do {
				List list = query.getResultList();
				assertEquals( 1, list.size() );

				results = query.hasMoreResults();
				// and it only sets the updateCount once lol
			} while ( results || updateCount != -1);
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-8416", reason = "JPA TCK challenge" )
	public void testHasMoreResultsHandlingTckChallenge() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
			assertTrue( query.execute() );
			assertTrue( query.hasMoreResults() );
			query.getResultList();
			assertFalse( query.hasMoreResults() );
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	public void testHasMoreResultsHandling() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
			assertTrue( query.execute() );
			query.getResultList();
			assertFalse( query.hasMoreResults() );
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	public void testResultClassHandling() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
			boolean isResult = query.execute();
			assertTrue( isResult );
			int updateCount = query.getUpdateCount();

			boolean results = false;
			do {
				List list = query.getResultList();
				assertEquals( 1, list.size() );
				assertTyping( User.class, list.get( 0 ) );

				results = query.hasMoreResults();
				// and it only sets the updateCount once lol
			} while ( results || updateCount != -1);
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	public void testSettingInParamDefinedOnNamedStoredProcedureQuery() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
			query.setParameter( 1, 1 );
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	public void testSettingNonExistingParams() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			// non-existing positional param
			try {
				StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
				query.setParameter( 99, 1 );
				fail( "Expecting an exception" );
			}
			catch (IllegalArgumentException expected) {
				// this is the expected condition
			}

			// non-existing named param
			try {
				StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
				query.setParameter( "does-not-exist", 1 );
				fail( "Expecting an exception" );
			}
			catch (IllegalArgumentException expected) {
				// this is the expected condition
			}
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-8395", reason = "Out of the frying pan into the fire: https://issues.apache.org/jira/browse/DERBY-211" )
	public void testExecuteUpdate() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		try {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "deleteAllUsers" );
			int count = query.executeUpdate();
			// this fails because the Derby EmbeddedDriver is returning zero here rather than the actual updateCount :(
			// https://issues.apache.org/jira/browse/DERBY-211
			assertEquals( 1, count );
		}
		finally {
			em.getTransaction().commit();
			em.close();
		}
	}

	public void testParameterRegistration() {

	}

	// todo : look at ways to allow "Auxiliary DB Objects" to the db via EMF bootstrapping.

//	public static final String findOneUser_CREATE_CMD = "CREATE ALIAS findOneUser AS $$\n" +
//			"import org.h2.tools.SimpleResultSet;\n" +
//			"import java.sql.*;\n" +
//			"@CODE\n" +
//			"ResultSet findOneUser() {\n" +
//			"    SimpleResultSet rs = new SimpleResultSet();\n" +
//			"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
//			"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
//			"    rs.addRow(1, \"Steve\");\n" +
//			"    return rs;\n" +
//			"}\n" +
//			"$$";
//	public static final String findOneUser_DROP_CMD = "DROP ALIAS findOneUser IF EXISTS";
//
//	public static final String deleteAllUsers_CREATE_CMD = "CREATE ALIAS deleteAllUsers AS $$\n" +
//			"@CODE\n" +
//			"int deleteAllUsers() {\n" +
//			"    return 156;" +
//			"}\n" +
//			"$$";
//	public static final String deleteAllUsers_DROP_CMD = "DROP ALIAS deleteAllUsers IF EXISTS";


	@Before
	public void startUp() {

		// create the procedures
		createTestUser( entityManagerFactory() );
		createProcedures( entityManagerFactory() );
	}


	@After
	public void tearDown() {

		deleteTestUser( entityManagerFactory() );
		dropProcedures( entityManagerFactory() );

	}

	private void createProcedures(SessionFactoryImplementor emf) {
		final JdbcConnectionAccess connectionAccess = emf.getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
		final Connection conn;
		try {
			conn = connectionAccess.obtainConnection();
			conn.setAutoCommit( false );

			try {
				Statement statement = conn.createStatement();

				// drop them, just to be sure
				try {
					dropProcedures( statement );
				}
				catch (SQLException ignore) {
				}

				createProcedureFindOneUser( statement );
				createProcedureDeleteAllUsers( statement );
				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					System.out.println( "Unable to commit transaction after creating creating procedures");
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to create stored procedures", e );
		}
	}

	private void dropProcedures(Statement statement) throws SQLException {
		statement.execute( "DROP PROCEDURE findOneUser" );
		statement.execute( "DROP PROCEDURE deleteAllUsers" );
	}

	private void createProcedureFindOneUser(Statement statement) throws SQLException {
		statement.execute(
				"CREATE PROCEDURE findOneUser() " +
						"language java " +
						"dynamic result sets 1 " +
						"external name 'org.hibernate.community.dialect.DerbyJpaTckUsageTest.findOneUser' " +
						"parameter style java"
		);
	}

	private void createProcedureDeleteAllUsers(Statement statement) throws SQLException {
		statement.execute(
				"CREATE PROCEDURE deleteAllUsers() " +
						"language java " +
						"external name 'org.hibernate.community.dialect.DerbyJpaTckUsageTest.deleteAllUsers' " +
						"parameter style java"
		);
	}

	public static void findOneUser(ResultSet[] results) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		PreparedStatement ps = conn.prepareStatement( "select id, name from t_user where name=?" );
		ps.setString( 1, "steve" );
		results[0] = ps.executeQuery();
		conn.close();
	}

	public static void findUserIds(ResultSet[] results) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		PreparedStatement ps = conn.prepareStatement( "select id from t_user" );
		results[0] = ps.executeQuery();
		conn.close();
	}

	public static void deleteAllUsers() throws SQLException {
		// afaict the only way to return update counts here is to actually perform some DML
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );
		System.out.println( "Preparing delete all" );
		PreparedStatement ps = conn.prepareStatement( "delete from t_user" );
		System.out.println( "Executing delete all" );
		int count = ps.executeUpdate();
		System.out.println( "Count : " + count );
		System.out.println( "Closing resources" );
		ps.close();
		conn.close();
	}

	private void createTestUser(SessionFactoryImplementor entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();

		em.persist( new User( 1, "steve" ) );
		em.getTransaction().commit();
		em.close();
	}

	private void deleteTestUser(SessionFactoryImplementor entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from User" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	private void dropProcedures(SessionFactoryImplementor emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
		final Connection conn;
		try {
			conn = connectionAccess.obtainConnection();
			conn.setAutoCommit( false );

			try {
				Statement statement = conn.createStatement();
				dropProcedures( statement );
				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					System.out.println( "Unable to commit transaction after creating dropping procedures");
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to drop stored procedures", e );
		}
	}
}
