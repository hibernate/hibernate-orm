/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi.delegation;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
public class SessionDelegatorBaseImplTest extends BaseCoreFunctionalTestCase {

	@Before
	public void init() {
		inTransaction( session -> {
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP ALIAS findOneUser IF EXISTS" );
					statement.executeUpdate(
							"CREATE ALIAS findOneUser AS $$\n" +
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
									"$$"
					);
				}
			} );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP ALIAS findOneUser IF EXISTS" );
				}
				catch (SQLException e) {
					//Do not ignore as failure to cleanup might lead to other tests to fail:
					throw new RuntimeException( e );
				}
			} );
		} );
	}

	@Test
	public void testcreateStoredProcedureQuery() {
		inTransaction(
				session -> {
					SessionDelegatorBaseImpl delegator = new SessionDelegatorBaseImpl( session );
					delegator.createStoredProcedureQuery( "findOneUser" );
				}
		);
	}
}
