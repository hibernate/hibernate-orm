/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.procedure;

import javax.persistence.EntityManager;
import javax.persistence.StoredProcedureQuery;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests various JPA usage scenarios for performing stored procedures.  Inspired by the awesomely well-done JPA TCK
 *
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class JpaUsageTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testMultipleGetUpdateCountCalls() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		StoredProcedureQuery query = em.createStoredProcedureQuery( "findOneUser" );
		// this is what the TCK attempts to do, don't shoot the messenger...
		query.getUpdateCount();
		// yep, twice
		int updateCount = query.getUpdateCount();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testBasicScalarResults() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

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

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-8398" )
	public void testResultClassHandling() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

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

		em.getTransaction().commit();
		em.close();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}

	// todo : look at ways to allow "Auxiliary DB Objects" to the db via EMF bootstrapping.

	public static final String CREATE_CMD = "CREATE ALIAS findOneUser AS $$\n" +
			"import org.h2.tools.SimpleResultSet;\n" +
			"import java.sql.*;\n" +
			"@CODE\n" +
			"ResultSet findOneUser() {\n" +
			"    SimpleResultSet rs = new SimpleResultSet();\n" +
			"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
			"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
			"    rs.addRow(1, \"Steve\");\n" +
			"    return rs;\n" +
			"}\n" +
			"$$";
	public static final String DROP_CMD = "DROP ALIAS findOneUser IF EXISTS";

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		execute( CREATE_CMD );
	}

	private void execute(String sql) {
		System.out.println( "Executing SQL : " + sql );
		final SessionFactoryImplementor sf = entityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final Connection conn;
		try {
			conn = sf.getConnectionProvider().getConnection();

			try {
				Statement statement = conn.createStatement();
				statement.execute( sql );
				try {
					statement.close();
				}
				catch (SQLException ignore) {
				}
			}
			finally {
				try {
					sf.getConnectionProvider().closeConnection( conn );
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to execute SQL : " + sql, e );
		}
	}

	@Override
	public void releaseResources() {
		execute( DROP_CMD );
		super.releaseResources();
	}
}
