/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.procedure.User;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests various JPA usage scenarios for performing stored procedures.  Inspired by the awesomely well-done JPA TCK
 *
 * @author Steve Ebersole
 */
@RequiresDialect(DerbyDialect.class)
@Jpa(
		annotatedClasses = {
				User.class
		}
)
public class DerbyJpaTckUsageTest {

	@Test
	public void testMultipleGetUpdateCountCalls(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "findOneUser" );
					// this is what the TCK attempts to do, don't shoot the messenger...
					query.getUpdateCount();
					// yep, twice
					query.getUpdateCount();
				}
		);
	}

	@Test
	public void testBasicScalarResults(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser" );
					boolean isResult = query.execute();
					assertThat( isResult ).isTrue();
					int updateCount = query.getUpdateCount();

					boolean results = false;
					do {
						List list = query.getResultList();
						assertThat( list ).hasSize( 1 );

						results = query.hasMoreResults();
						// and it only sets the updateCount once lol
					}
					while ( results || updateCount != -1 );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-8416", reason = "JPA TCK challenge")
	public void testHasMoreResultsHandlingTckChallenge(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
					assertThat( query.execute() ).isTrue();
					assertThat( query.hasMoreResults() ).isTrue();
					query.getResultList();
					assertThat( query.hasMoreResults() ).isFalse();
				}
		);
	}

	@Test
	public void testHasMoreResultsHandling(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
					assertThat( query.execute() ).isTrue();
					query.getResultList();
					assertThat( query.hasMoreResults() ).isFalse();
				}
		);
	}

	@Test
	public void testResultClassHandling(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser", User.class );
					boolean isResult = query.execute();
					assertThat( isResult ).isTrue();
					int updateCount = query.getUpdateCount();

					boolean results = false;
					do {
						List list = query.getResultList();
						assertThat( list ).hasSize( 1 );
						assertTyping( User.class, list.get( 0 ) );

						results = query.hasMoreResults();
						// and it only sets the updateCount once lol
					}
					while ( results || updateCount != -1 );
				}
		);
	}

	@Test
	public void testSettingInParamDefinedOnNamedStoredProcedureQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
					query.setParameter( 1, 1 );
				}
		);
	}

	@Test
	public void testSettingNonExistingParams(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					// non-existing positional param
					assertThrows( IllegalArgumentException.class, () -> {
						StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
						query.setParameter( 99, 1 );
					} );

					// non-existing named param
					assertThrows( IllegalArgumentException.class, () -> {
						StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "positional-param" );
						query.setParameter( "does-not-exist", 1 );
						fail( "Expecting an exception" );
					} );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-8395",
			reason = "Out of the frying pan into the fire: https://issues.apache.org/jira/browse/DERBY-211")
	public void testExecuteUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					StoredProcedureQuery query = em.createStoredProcedureQuery( "deleteAllUsers" );
					int count = query.executeUpdate();
					// this fails because the Derby EmbeddedDriver is returning zero here rather than the actual updateCount :(
					// https://issues.apache.org/jira/browse/DERBY-211
					assertThat( count ).isEqualTo( 1 );
				}
		);
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


	@BeforeEach
	public void startUp(EntityManagerFactoryScope scope) {
		// create the procedures
		createTestUser( scope );
		createProcedures( scope );
	}


	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		deleteTestUser( scope );
		dropProcedures( scope );
	}

	private void createProcedures(EntityManagerFactoryScope scope) {
		SessionFactoryImplementor sessionFactoryImplementor = scope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sessionFactoryImplementor.getServiceRegistry()
				.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
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
					System.out.println( "Unable to commit transaction after creating creating procedures" );
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

	private void createTestUser(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> em.persist( new User( 1, "steve" ) )
		);
	}

	private void deleteTestUser(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> em.createQuery( "delete from User" ).executeUpdate()
		);
	}

	private void dropProcedures(EntityManagerFactoryScope scope) {
		final SessionFactoryImplementor sf = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry().getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
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
					System.out.println( "Unable to commit transaction after creating dropping procedures" );
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
