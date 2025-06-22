/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
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
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(value = HSQLDialect.class)
public class HSQLStoreProcedureTest extends BaseEntityManagerFunctionalTestCase {
	EntityManagerFactory entityManagerFactory;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Before
	public void startUp() {
		entityManagerFactory = getOrCreateEntityManager().getEntityManagerFactory();

		createProcedures( entityManagerFactory );
	}

	@After
	public void tearDown() {
		dropProcedures( entityManagerFactory );
	}

	@Test
	@JiraKey(value = "HHH-10515")
	public void testNamedStoredProcedureExecution() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			query.setParameter( "arg1", 1 );
			query.execute();
		}
		finally {
			em.close();
		}
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetNamedParameters() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			assertThat( query.getParameter( "arg1" ), not( nullValue() ) );
			assertThat( query.getParameter( "res" ), not( nullValue() ) );
			assertThat( query.getParameter( "arg1", Integer.class ), not( nullValue() ) );
			try {
				query.getParameter( "arg1", String.class );
				fail( "An IllegalArgumentException is expected, A parameter with name arg1 and type String does not exist" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}

			try {
				query.getParameter( "arg2" );
				fail( "An IllegalArgumentException is expected, A parameter with name arg2 does not exist" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}
		}
		finally {
			em.close();
		}
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetPositionalParameters() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutproc" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			try {
				query.getParameter( 1 );
				fail( "An IllegalArgumentException is expected, The stored procedure has named parameters not positional" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}
			try {
				query.getParameter( 1, String.class );
				fail( "An IllegalArgumentException is expected, The stored procedure has named parameters not positional" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}
		}
		finally {
			em.close();
		}
	}

	@Test
	@JiraKey(value = "HHH-10915")
	public void testGetPositionalParameters2() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.inoutprocpositional" );
			final Set<Parameter<?>> parameters = query.getParameters();
			assertThat( parameters.size(), is( 2 ) );
			assertThat( query.getParameter( 1 ), not( nullValue() ) );
			assertThat( query.getParameter( 2 ), not( nullValue() ) );
			assertThat( query.getParameter( 1, Integer.class ), not( nullValue() ) );
			try {
				query.getParameter( 3 );
				fail( "An IllegalArgumentException is expected, A parameter at position 3 does not exist" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}

			try {
				query.getParameter( 1, String.class );
				fail( "An IllegalArgumentException is expected, The parameter at position 1 is of type Integer not String" );
			}
			catch (IllegalArgumentException iae) {
				//expected
			}

		}
		finally {
			em.close();
		}
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
	public class User {

		@Id
		@GeneratedValue
		private Integer id;
	}
}
