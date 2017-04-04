/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-10761")
@RequiresDialect(Oracle10gDialect.class)
public class StoredProcedureNullParameterByNameTest extends BaseEntityManagerFunctionalTestCase {
	EntityManagerFactory entityManagerFactory;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void addConfigOptions(Map options) {
		options.put( org.hibernate.cfg.AvailableSettings.PROCEDURE_NULL_PARAM_PASSING, "true" );
	}

	@Before
	public void startUp() {
		entityManagerFactory = getOrCreateEntityManager().getEntityManagerFactory();

		createProcedures( entityManagerFactory );
	}

	@Test
	public void testPassNull() {
		EntityManager em = entityManagerFactory.createEntityManager();
		em.getTransaction().begin();
		User user1 = new User();
		user1.id = 1;
		user1.name = "aName";
		user1.age = 99;
		em.persist( user1 );
		em.getTransaction().commit();

		em.clear();

		try {
			StoredProcedureQuery query = em.createNamedStoredProcedureQuery( "User.findNameById" );
			query.setParameter( "ID_PARAM", 1 );
			// null is passed for EXTRA_IN_PARAM

			assertEquals( "aName", query.getOutputParameterValue( "NAME_PARAM" ) );
			assertEquals( null, query.getOutputParameterValue( "EXTRA_OUT_PARAM" ) );
		}
		finally {
			em.close();
		}
	}


	private void createProcedures(EntityManagerFactory emf) {
		createProcedure(
				emf,
				"CREATE OR REPLACE PROCEDURE PROC_EXAMPLE_ONE_BASIC_OUT ( " +
						"  ID_PARAM IN NUMBER, EXTRA_IN_PARAM IN NUMBER, NAME_PARAM OUT VARCHAR2, EXTRA_OUT_PARAM OUT NUMBER ) " +
						"AS " +
						"BEGIN " +
						"  SELECT NAME, EXTRA_IN_PARAM INTO NAME_PARAM, EXTRA_OUT_PARAM FROM USERS WHERE id = ID_PARAM; " +
						"END PROC_EXAMPLE_ONE_BASIC_OUT; "
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
			@NamedStoredProcedureQuery(name = "User.findNameById",
					resultClasses = User.class,
					procedureName = "PROC_EXAMPLE_ONE_BASIC_OUT",
					parameters = {
							@StoredProcedureParameter(mode = ParameterMode.IN, name = "ID_PARAM", type = Integer.class),
							@StoredProcedureParameter(mode = ParameterMode.IN, name = "EXTRA_IN_PARAM", type = Integer.class),
							@StoredProcedureParameter(mode = ParameterMode.OUT, name = "NAME_PARAM", type = String.class),
							@StoredProcedureParameter(mode = ParameterMode.OUT, name = "EXTRA_OUT_PARAM", type = Integer.class)
					}
			)
	)
	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User {
		@Id
		private Integer id;

		@Column(name = "NAME")
		private String name;

		@Column(name = "AGE")
		private Integer age;
	}
}
