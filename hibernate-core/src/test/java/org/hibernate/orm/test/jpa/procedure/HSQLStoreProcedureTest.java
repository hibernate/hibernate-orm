/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(value = HSQLDialect.class)
@Jpa(annotatedClasses = {HSQLStoreProcedureTest.User.class})
public class HSQLStoreProcedureTest {

	@BeforeEach
	public void startUp(EntityManagerFactoryScope scope) {
		createProcedures( scope.getEntityManagerFactory() );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		dropProcedures( scope.getEntityManagerFactory() );
	}

	@Test
	@JiraKey(value = "HHH-10515")
	public void testNamedStoredProcedureExecution(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			query.setParameter( "arg1", 1 );
			query.execute();
		} );
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetNamedParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			assertThat( query.getParameter( "arg1" ), not( nullValue() ) );
			assertThat( query.getParameter( "res" ), not( nullValue() ) );
			assertThat( query.getParameter( "arg1", Integer.class ), not( nullValue() ) );

			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( "arg1", String.class ),
					"An IllegalArgumentException is expected, A parameter with name arg1 and type String does not exist"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( "arg2" ),
					"An IllegalArgumentException is expected, A parameter with name arg2 does not exist"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetPositionalParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( 1 ),
					"An IllegalArgumentException is expected, The stored procedure has named parameters not positional"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( 1, String.class ),
					"An IllegalArgumentException is expected, The stored procedure has named parameters not positional"
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetPositionalParameters2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutprocpositional" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			assertThat( query.getParameter( 1 ), not( nullValue() ) );
			assertThat( query.getParameter( 2 ), not( nullValue() ) );
			assertThat( query.getParameter( 1, Integer.class ), not( nullValue() ) );
			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( 3 ),
					"An IllegalArgumentException is expected, A parameter at position 3 does not exist"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> query.getParameter( 1, String.class ),
					"An IllegalArgumentException is expected, The parameter at position 1 is of type Integer not String"
			);
		} );
	}

	private void createProcedures(EntityManagerFactory emf) {
		final String procedureStatement = "CREATE procedure inoutproc (IN arg1 int, OUT res int) " +
				"BEGIN ATOMIC set res = arg1 + 1;" +
				"END";
		executeStatement( emf, procedureStatement );
	}

	private void dropProcedures(EntityManagerFactory emf) {
		executeStatement( emf, "DROP procedure inoutproc" );
	}

	public void executeStatement(EntityManagerFactory emf, String toExecute) {
		final SessionFactoryImplementor sf = emf.unwrap( SessionFactoryImplementor.class );
		final JdbcConnectionAccess connectionAccess = sf.getServiceRegistry()
				.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection conn;
		try {
			conn = connectionAccess.obtainConnection();
			conn.setAutoCommit( false );

			try {
				Statement statement = conn.createStatement();
				statement.execute( toExecute );

				try {
					statement.close();
				}
				catch (SQLException e) {
					fail( e.getMessage() );
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					fail( e.getMessage() );
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException e) {
					fail( e.getMessage() );
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to create stored procedures", e );
		}
	}

	@Entity(name = "User")
	@NamedStoredProcedureQueries(value = {
			@NamedStoredProcedureQuery(name = "User.inoutproc", procedureName = "inoutproc", parameters = {
					@StoredProcedureParameter(mode = ParameterMode.IN, name = "arg1", type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class)
			})
			,
			@NamedStoredProcedureQuery(name = "User.inoutprocpositional", procedureName = "inoutproc", parameters = {
					@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
					@StoredProcedureParameter(mode = ParameterMode.OUT, type = Integer.class)
			})
	}
	)
	@Table(name = "USERS")
	public static class User {
		@Id
		@GeneratedValue
		private Integer id;
	}
}
