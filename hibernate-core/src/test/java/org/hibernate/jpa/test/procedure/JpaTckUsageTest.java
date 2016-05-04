/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
public class JpaTckUsageTest extends BaseUnitTestCase {

	@Test
	public void testMultipleGetUpdateCountCalls() {
		EntityManager em = entityManagerFactory.createEntityManager();
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
		EntityManager em = entityManagerFactory.createEntityManager();
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
	@FailureExpected( jiraKey = "HHH-8416", message = "JPA TCK challenge" )
	public void testHasMoreResultsHandlingTckChallenge() {
		EntityManager em = entityManagerFactory.createEntityManager();
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
		EntityManager em = entityManagerFactory.createEntityManager();
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
		EntityManager em = entityManagerFactory.createEntityManager();
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
		EntityManager em = entityManagerFactory.createEntityManager();
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
		EntityManager em = entityManagerFactory.createEntityManager();
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
	@FailureExpected( jiraKey = "HHH-8395", message = "Out of the frying pan into the fire: https://issues.apache.org/jira/browse/DERBY-211" )
	public void testExecuteUpdate() {
		EntityManager em = entityManagerFactory.createEntityManager();
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

	HibernateEntityManagerFactory entityManagerFactory;

	@Before
	public void startUp() {
		// create the EMF
		entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				buildSettingsMap()
		).build().unwrap( HibernateEntityManagerFactory.class );

		// create the procedures
		createTestUser( entityManagerFactory );
		createProcedures( entityManagerFactory );
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	@SuppressWarnings("unchecked")
	private Map buildSettingsMap() {
		Map settings = new HashMap();

		settings.put( AvailableSettings.LOADED_CLASSES, Collections.singletonList( User.class ) );

		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, DerbyTenSevenDialect.class );
		settings.put( org.hibernate.cfg.AvailableSettings.DRIVER,  org.apache.derby.jdbc.EmbeddedDriver.class.getName() );
//		settings.put( org.hibernate.cfg.AvailableSettings.URL, "jdbc:derby:/tmp/hibernate-orm-testing;create=true" );
		settings.put( org.hibernate.cfg.AvailableSettings.URL, "jdbc:derby:memory:hibernate-orm-testing;create=true" );
		settings.put( org.hibernate.cfg.AvailableSettings.USER, "" );

		settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, DerbyTenSevenDialect.class.getName() );
		return settings;
	}

	@After
	public void tearDown() {
		if ( entityManagerFactory == null ) {
			return;
		}

		deleteTestUser( entityManagerFactory );
		dropProcedures( entityManagerFactory );

		entityManagerFactory.close();
	}

	private void createProcedures(HibernateEntityManagerFactory emf) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry().getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();
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
					System.out.println( "Unable to commit transaction afterQuery creating creating procedures");
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
						"external name 'org.hibernate.jpa.test.procedure.JpaTckUsageTest.findOneUser' " +
						"parameter style java"
		);
	}

	private void createProcedureDeleteAllUsers(Statement statement) throws SQLException {
		statement.execute(
				"CREATE PROCEDURE deleteAllUsers() " +
						"language java " +
						"external name 'org.hibernate.jpa.test.procedure.JpaTckUsageTest.deleteAllUsers' " +
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

	private void createTestUser(HibernateEntityManagerFactory entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();

		em.persist( new User( 1, "steve" ) );
		em.getTransaction().commit();
		em.close();
	}

	private void deleteTestUser(HibernateEntityManagerFactory entityManagerFactory) {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from User" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	private void dropProcedures(HibernateEntityManagerFactory emf) {
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
					System.out.println( "Unable to commit transaction afterQuery creating dropping procedures");
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
