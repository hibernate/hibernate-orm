/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.refcursor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect( Oracle8iDialect.class )
public class CursorFromCallableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NumValue.class };
	}

	@Before
	public void createRefCursorFunction() {
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
				"END f_test_return_cursor;" );
	}

	@After
	public void dropRefCursorFunction() {
		executeStatement( "DROP FUNCTION f_test_return_cursor" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8022" )
	public void testReadResultSetFromRefCursor() {
		Session session = openSession();
		session.getTransaction().begin();

		Assert.assertEquals(
				Arrays.asList( new NumValue( 1, "Line 1" ), new NumValue( 2, "Line 2" ) ),
				session.getNamedQuery( "NumValue.getSomeValues" ).list()
		);

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7984" )
	public void testStatementClosing() {
		Session session = openSession();
		session.getTransaction().begin();
		// Reading maximum number of opened cursors requires SYS privileges.
		// Verify statement closing with JdbcCoordinator#hasRegisteredResources() instead.
		// BigDecimal maxCursors = (BigDecimal) session.createSQLQuery( "SELECT value FROM v$parameter WHERE name = 'open_cursors'" ).uniqueResult();
		// for ( int i = 0; i < maxCursors + 10; ++i ) { named_query_execution }
		Assert.assertEquals(
				Arrays.asList( new NumValue( 1, "Line 1" ), new NumValue( 2, "Line 2" ) ),
				session.getNamedQuery( "NumValue.getSomeValues" ).list()
		);
		JdbcCoordinator jdbcCoordinator = ( (SessionImplementor) session ).getJdbcCoordinator();
		Assert.assertFalse(
				"Prepared statement and result set should be released after query execution.",
				jdbcCoordinator.getResourceRegistry().hasRegisteredResources()
		);
		session.getTransaction().commit();
		session.close();
	}

	private void executeStatement(final String sql) {
		final Session session = openSession();
		session.getTransaction().begin();

		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				final JdbcCoordinator jdbcCoordinator = ( (SessionImplementor) session ).getJdbcCoordinator();
				final StatementPreparer statementPreparer = jdbcCoordinator.getStatementPreparer();
				final ResultSetReturn resultSetReturn = jdbcCoordinator.getResultSetReturn();
				PreparedStatement preparedStatement = null;
				try {
					preparedStatement = statementPreparer.prepareStatement( sql );
					resultSetReturn.execute( preparedStatement );
				}
				finally {
					if ( preparedStatement != null ) {
						try {
							jdbcCoordinator.getResourceRegistry().release( preparedStatement );
						}
						catch ( Throwable ignore ) {
							// ignore...
						}
					}
				}
			}
		} );

		session.getTransaction().commit();
		session.close();
	}
}
