/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jdbc.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.boot.BasicTestingJdbcServiceImpl;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class AggressiveReleaseTest extends BaseSessionFactoryFunctionalTest {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
			true
	);

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
		builer.applySetting(
				AvailableSettings.CONNECTION_HANDLING,
				PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT
		);
	}

	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	@BeforeEach
	protected void prepareTest() throws Exception {

		services.prepare( true );

		Connection connection = null;
		Statement stmnt = null;
		try {
			connection = services.getBootstrapJdbcConnectionAccess().obtainConnection();
			stmnt = connection.createStatement();
			stmnt.execute( "drop table SANDBOX_JDBC_TST if exists" );
			stmnt.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		}
		finally {
			if ( stmnt != null ) {
				try {
					stmnt.close();
				}
				catch (SQLException ignore) {
				}
			}
			if ( connection != null ) {
				try {
					services.getBootstrapJdbcConnectionAccess().releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}
	}

	@AfterEach
	protected void cleanupTest() throws Exception {
		Connection connection = null;
		Statement stmnt = null;
		try {
			connection = services.getBootstrapJdbcConnectionAccess().obtainConnection();
			stmnt = connection.createStatement();
			stmnt.execute( "drop table SANDBOX_JDBC_TST if exists" );
		}
		finally {
			if ( stmnt != null ) {
				try {
					stmnt.close();
				}
				catch (SQLException ignore) {
				}
			}
			if ( connection != null ) {
				try {
					services.getBootstrapJdbcConnectionAccess().releaseConnection( connection );
				}
				catch (SQLException ignore) {
				}
			}
		}

		services.release();
	}

	@Test
	public void testBasicRelease() {
		ResourceRegistry registry = sessionFactoryScope().fromSession(
				session -> {
					connectionProvider.clear();
					JdbcCoordinatorImpl jdbcCoord = (JdbcCoordinatorImpl) session.getJdbcCoordinator();
					ResourceRegistry resourceRegistry = jdbcCoord.getLogicalConnection().getResourceRegistry();
					try {
						String sql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";
						PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement(
								sql );
						ps.setLong( 1, 1 );
						ps.setString( 2, "name" );
						jdbcCoord.getResultSetReturn().execute( ps, sql );
                        assertTrue( jdbcCoord.getLogicalConnection().getResourceRegistry().hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 0, connectionProvider.getReleasedConnections().size() );
						resourceRegistry.release( ps );
						jdbcCoord.afterStatementExecution();

						assertFalse( resourceRegistry.hasRegisteredResources() );
						assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );
					}
					catch (SQLException sqle) {
						fail( "incorrect exception type : sqlexception" );
					}
					finally {
						jdbcCoord.close();
					}
					return resourceRegistry;
				}
		);

		assertFalse( registry.hasRegisteredResources() );
	}

	@Test
	public void testReleaseCircumventedByHeldResources() {
		ResourceRegistry registry = sessionFactoryScope().fromSession(
				session -> {
					connectionProvider.clear();
					JdbcCoordinatorImpl jdbcCoord = (JdbcCoordinatorImpl) session.getJdbcCoordinator();
					ResourceRegistry resourceRegistry = jdbcCoord.getLogicalConnection().getResourceRegistry();

					try {
						String sql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";
						PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement(
								sql );
						ps.setLong( 1, 1 );
						ps.setString( 2, "name" );
						jdbcCoord.getResultSetReturn().execute( ps , sql);
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 0, connectionProvider.getReleasedConnections().size() );
						resourceRegistry.release( ps );
						jdbcCoord.afterStatementExecution();

						assertFalse( resourceRegistry.hasRegisteredResources() );
						assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );

						// open a result set and hold it open...
						sql = "select * from SANDBOX_JDBC_TST";
						ps = jdbcCoord.getStatementPreparer().prepareStatement( sql );
						jdbcCoord.getResultSetReturn().extract( ps, sql );
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );

						// open a second result set
						PreparedStatement ps2 = jdbcCoord.getStatementPreparer().prepareStatement( sql );
						jdbcCoord.getResultSetReturn().execute( ps, sql );
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );
						// and close it...
						resourceRegistry.release( ps2 );
						jdbcCoord.afterStatementExecution();
						// the release should be circumvented...
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );
						// let the close of the logical connection below release all resources (hopefully)...
					}
					catch (SQLException sqle) {
						fail( "incorrect exception type : sqlexception" );
					}
					finally {
						jdbcCoord.close();

					}
					return resourceRegistry;

				} );

		assertFalse( registry.hasRegisteredResources() );
		assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
		assertEquals( 2, connectionProvider.getReleasedConnections().size() );
	}

	@Test
	public void testReleaseCircumventedManually() {
		ResourceRegistry registry = sessionFactoryScope().fromSession(
				session -> {
					connectionProvider.clear();
					JdbcCoordinatorImpl jdbcCoord = (JdbcCoordinatorImpl) session.getJdbcCoordinator();
					ResourceRegistry resourceRegistry = jdbcCoord.getLogicalConnection().getResourceRegistry();

					try {
						String sql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";
						PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement(
								sql );
						ps.setLong( 1, 1 );
						ps.setString( 2, "name" );
						jdbcCoord.getResultSetReturn().execute( ps , sql);
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 0, connectionProvider.getReleasedConnections().size() );
						resourceRegistry.release( ps );
						jdbcCoord.afterStatementExecution();
						assertFalse( resourceRegistry.hasRegisteredResources() );
						assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );

						// disable releases...
						jdbcCoord.disableReleases();

						// open a result set...
						sql = "select * from SANDBOX_JDBC_TST";
						ps = jdbcCoord.getStatementPreparer().prepareStatement( sql );
						jdbcCoord.getResultSetReturn().extract( ps, sql );
						assertTrue( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );
						// and close it...
						resourceRegistry.release( ps );
						jdbcCoord.afterStatementExecution();
						// the release should be circumvented...
						assertFalse( resourceRegistry.hasRegisteredResources() );
						assertEquals( 1, connectionProvider.getAcquiredConnections().size() );
						assertEquals( 1, connectionProvider.getReleasedConnections().size() );

						// let the close of the logical connection below release all resources (hopefully)...
					}
					catch (SQLException sqle) {
						fail( "incorrect exception type : sqlexception" );
					}
					finally {
						jdbcCoord.close();
					}
					return resourceRegistry;
				} );

		assertFalse( registry.hasRegisteredResources() );
		assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
		assertEquals( 2, connectionProvider.getReleasedConnections().size() );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19477")
	public void testHql() {
		sessionFactoryScope().inTransaction( session -> {
			connectionProvider.clear();
			JdbcCoordinatorImpl jdbcCoord = (JdbcCoordinatorImpl) session.getJdbcCoordinator();
			ResourceRegistry resourceRegistry = jdbcCoord.getLogicalConnection().getResourceRegistry();

			session.createSelectionQuery( "select 1" ).uniqueResult();

			assertFalse( resourceRegistry.hasRegisteredResources() );
			assertEquals( 0, connectionProvider.getAcquiredConnections().size() );
			assertEquals( 1, connectionProvider.getReleasedConnections().size() );
		} );
	}
}
