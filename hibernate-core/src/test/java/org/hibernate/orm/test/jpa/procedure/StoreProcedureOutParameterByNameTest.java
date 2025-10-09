/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 * @author Gail Badner
 */
@JiraKey(value = "HHH-10756")
@RequiresDialect(OracleDialect.class)
@Jpa( annotatedClasses = StoreProcedureOutParameterByNameTest.User.class)
public class StoreProcedureOutParameterByNameTest {

	@BeforeEach
	public void startUp(EntityManagerFactoryScope scope) {
		createProcedures( scope.getEntityManagerFactory() );
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete from User" ).executeUpdate();
			}
		);
	}

	@Test
	public void testOneBasicOutParameter(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			User user = new User();
			user.id = 1;
			user.name = "aName";
			em.persist( user );
			em.getTransaction().commit();

			em.clear();

			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.findNameById" );
			query.setParameter( "ID_PARAM", 1 );

			assertEquals( "aName", query.getOutputParameterValue( "NAME_PARAM" ) );
		} );
	}

	@Test
	public void testTwoBasicOutParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			User user = new User();
			user.id = 1;
			user.name = "aName";
			user.age = 29;
			em.persist( user );
			em.getTransaction().commit();

			em.clear();

			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.findNameAndAgeById" );
			query.setParameter( "ID_PARAM", 1 );

			assertEquals( "aName", query.getOutputParameterValue( "NAME_PARAM" ) );
			assertEquals( 29, query.getOutputParameterValue( "AGE_PARAM" ) );
		} );
	}

	private void createProcedures(EntityManagerFactory emf) {
		createProcedure(
				emf,
				"CREATE OR REPLACE PROCEDURE PROC_EXAMPLE_ONE_BASIC_OUT ( " +
						"  ID_PARAM IN NUMBER, NAME_PARAM OUT VARCHAR2 ) " +
						"AS " +
						"BEGIN " +
						"  SELECT NAME INTO NAME_PARAM FROM USERS WHERE id = ID_PARAM; " +
						"END PROC_EXAMPLE_ONE_BASIC_OUT; "
		);

		createProcedure(
				emf,
				"CREATE OR REPLACE PROCEDURE PROC_EXAMPLE_TWO_BASIC_OUT ( " +
						"  ID_PARAM IN NUMBER, NAME_PARAM OUT VARCHAR2, AGE_PARAM OUT NUMBER ) " +
						"AS " +
						"BEGIN " +
						"  SELECT NAME, AGE INTO NAME_PARAM, AGE_PARAM FROM USERS WHERE id = ID_PARAM; " +
						"END PROC_EXAMPLE_TWO_BASIC_OUT; "
		);
	}

	private void createProcedure(EntityManagerFactory emf, String storedProc) {
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

				statement.execute( storedProc );

				try {
					statement.close();
				}
				catch (SQLException ignore) {
					fail();
				}
			}
			finally {
				try {
					conn.commit();
				}
				catch (SQLException e) {
					System.out.println( "Unable to commit transaction after creating creating procedures" );
					fail();
				}

				try {
					connectionAccess.releaseConnection( conn );
				}
				catch (SQLException ignore) {
					fail();
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to create stored procedures", e );
		}
	}

	@NamedStoredProcedureQueries(
			value = {
					@NamedStoredProcedureQuery(name = "User.findNameById",
							resultClasses = User.class,
							procedureName = "PROC_EXAMPLE_ONE_BASIC_OUT",
							parameters = {
									@StoredProcedureParameter(mode = ParameterMode.IN, name = "ID_PARAM", type = Integer.class),
									@StoredProcedureParameter(mode = ParameterMode.OUT, name = "NAME_PARAM", type = String.class)
							}
					),
					@NamedStoredProcedureQuery(name = "User.findNameAndAgeById",
						resultClasses = User.class,
						procedureName = "PROC_EXAMPLE_TWO_BASIC_OUT",
						parameters = {
								@StoredProcedureParameter(mode = ParameterMode.IN, name = "ID_PARAM", type = Integer.class),
								@StoredProcedureParameter(mode = ParameterMode.OUT, name = "NAME_PARAM", type = String.class),
								@StoredProcedureParameter(mode = ParameterMode.OUT, name = "AGE_PARAM", type = Integer.class)
							}
					)
			}
	)
	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User {
		@Id
		private Integer id;

		@Column(name = "NAME")
		private String name;

		@Column(name = "AGE")
		private int age;
	}
}
