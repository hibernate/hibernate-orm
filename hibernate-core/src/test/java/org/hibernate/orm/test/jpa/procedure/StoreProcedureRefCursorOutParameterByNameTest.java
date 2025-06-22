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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9286")
@RequiresDialect(OracleDialect.class)
public class StoreProcedureRefCursorOutParameterByNameTest extends BaseEntityManagerFunctionalTestCase {
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

	@Test
	public void testNamedStoredProcedureExecution() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.findByName" );
			query.setParameter( "USER_NAME_PARAM", "my_name" );

			query.getResultList();
		}
		finally {
			em.close();
		}
	}

	private void createProcedures(EntityManagerFactory emf) {
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

				statement.execute(
						"CREATE OR REPLACE PROCEDURE PROC_EXAMPLE ( " +
								"  USER_NAME_PARAM IN VARCHAR2, CURSOR_PARAM OUT SYS_REFCURSOR ) " +
								"AS " +
								"BEGIN " +
								"  OPEN CURSOR_PARAM FOR " +
								"  SELECT * FROM USERS WHERE NAME = USER_NAME_PARAM; " +
								"END PROC_EXAMPLE; "
				);

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

	@NamedStoredProcedureQuery(name = "User.findByName",
			resultClasses = User.class,
			procedureName = "PROC_EXAMPLE"
			,
			parameters = {
					@StoredProcedureParameter(mode = ParameterMode.IN, name = "USER_NAME_PARAM", type = String.class),
					@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, name = "CURSOR_PARAM", type = Class.class)
			}
	)
	@Entity(name = "Message")
	@Table(name = "USERS")
	public static class User {
		@Id
		private Integer id;

		@Column(name = "NAME")
		private String name;
	}
}
