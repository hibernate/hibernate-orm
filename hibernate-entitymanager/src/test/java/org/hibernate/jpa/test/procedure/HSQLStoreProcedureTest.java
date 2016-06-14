/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.procedure;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10515")
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
	@NamedStoredProcedureQuery(name = "User.inoutproc", procedureName = "inoutproc", parameters = {
			@StoredProcedureParameter(mode = ParameterMode.IN, name = "arg1", type = Integer.class),
			@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class)
	})
	@Table(name = "USERS")
	public class User {

		@Id
		@GeneratedValue
		private Integer id;
	}
}
