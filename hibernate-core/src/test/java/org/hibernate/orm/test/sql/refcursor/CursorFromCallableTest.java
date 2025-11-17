/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.refcursor;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Lukasz Antoniak
 */
@RequiresDialect(OracleDialect.class)
@DomainModel(
		annotatedClasses = {
				NumValue.class
		}
)
@SessionFactory
public class CursorFromCallableTest {

	@BeforeAll
	public void createRefCursorFunction(SessionFactoryScope scope) {
		executeStatement( "CREATE OR REPLACE FUNCTION f_test_return_cursor RETURN SYS_REFCURSOR " +
						"IS " +
						"    l_Cursor SYS_REFCURSOR; " +
						"BEGIN " +
						"    OPEN l_Cursor FOR " +
						"      SELECT 1 AS BOT_NUM " +
						"           , 'Line 1' AS BOT_VALUE " +
						"        FROM DUAL " +
						"      UNION " +
						"      SELECT 2 AS BOT_NUM " +
						"           , 'Line 2' AS BOT_VALUE " +
						"        FROM DUAL; " +
						"    RETURN(l_Cursor); " +
						"END f_test_return_cursor;"
				,
				scope );
	}

	@AfterAll
	public void dropRefCursorFunction(SessionFactoryScope scope) {
		executeStatement( "DROP FUNCTION f_test_return_cursor", scope );
	}

	@Test
	@JiraKey(value = "HHH-8022")
	public void testReadResultSetFromRefCursor(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertThat( session.createNamedStoredProcedureQuery( "NumValue.getSomeValues" ).getResultList() )
							.isEqualTo( Arrays.asList( new NumValue( 1, "Line 1" ), new NumValue( 2, "Line 2" ) ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7984")
	public void testStatementClosing(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Reading maximum number of opened cursors requires SYS privileges.
					// Verify statement closing with JdbcCoordinator#hasRegisteredResources() instead.
					// BigDecimal maxCursors = (BigDecimal) session.createSQLQuery( "SELECT value FROM v$parameter WHERE name = 'open_cursors'" ).uniqueResult();
					// for ( int i = 0; i < maxCursors + 10; ++i ) { named_query_execution }
					ProcedureCall namedStoredProcedureQuery = session.createNamedStoredProcedureQuery(
							"NumValue.getSomeValues" );
					List resultList = namedStoredProcedureQuery.getResultList();
					assertThat( resultList )
							.isEqualTo( Arrays.asList( new NumValue( 1, "Line 1" ), new NumValue( 2, "Line 2" ) ) );
					namedStoredProcedureQuery.close();
					JdbcCoordinator jdbcCoordinator = ((SessionImplementor) session).getJdbcCoordinator();
					assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() )
							.describedAs("Prepared statement and result set should be released after query execution." )
							.isFalse();
				}
		);
	}

	private void executeStatement(final String sql, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.doWork( new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							final JdbcCoordinator jdbcCoordinator = ((SessionImplementor) session).getJdbcCoordinator();
							final StatementPreparer statementPreparer = jdbcCoordinator.getStatementPreparer();
							final ResultSetReturn resultSetReturn = jdbcCoordinator.getResultSetReturn();
							PreparedStatement preparedStatement = null;
							try {
								preparedStatement = statementPreparer.prepareStatement( sql );
								resultSetReturn.execute( preparedStatement, sql );
							}
							finally {
								if ( preparedStatement != null ) {
									try {
										jdbcCoordinator.getLogicalConnection().getResourceRegistry()
												.release( preparedStatement );
									}
									catch (Throwable ignore) {
										// ignore...
									}
								}
							}
						}
					} );
				}
		);
	}
}
