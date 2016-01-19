/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9286")
@RequiresDialect(Oracle10gDialect.class)
public class RefCursorOutParameterStoreProcedureTest extends BaseEntityManagerFunctionalTestCase {
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

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	@Test
	public void testNamedStoredProcedureExecution() {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.findByName" );
			query.setParameter( 1, "my_name" );

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
					@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class),
					@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Class.class)
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
