/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.delegation;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(H2Dialect.class)
@DomainModel
@SessionFactory
public class SessionDelegatorBaseImplTest {

	@BeforeEach
	public void init(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.doWork(
						(connection) -> {
							try ( Statement statement = connection.createStatement() ) {
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
						}
				)
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.doWork(
						(connection) -> {
							try (Statement statement = connection.createStatement()) {
								statement.executeUpdate( "DROP ALIAS findOneUser IF EXISTS" );
							}
							catch (SQLException e) {
								//Do not ignore as failure to cleanup might lead to other tests to fail:
								throw new RuntimeException( e );
							}
						}
				)
		);
	}

	@Test
	public void testCreateStoredProcedureQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					SessionDelegatorBaseImpl delegator = new SessionDelegatorBaseImpl( session );
					delegator.createStoredProcedureQuery( "findOneUser" );
				}
		);
	}
}
