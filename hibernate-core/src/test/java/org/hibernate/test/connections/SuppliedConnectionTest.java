// $Id: SuppliedConnectionTest.java 11332 2007-03-22 17:34:55Z steve.ebersole@jboss.com $
package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.ResultSet;

import junit.framework.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.connection.UserSuppliedConnectionProvider;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * Implementation of SuppliedConnectionTest.
 *
 * @author Steve Ebersole
 */
public class SuppliedConnectionTest extends ConnectionManagementTestCase {

	private ConnectionProvider cp = ConnectionProviderFactory.newConnectionProvider();
	private Connection connectionUnderTest;

	public SuppliedConnectionTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SuppliedConnectionTest.class );
	}

	protected Session getSessionUnderTest() throws Throwable {
		connectionUnderTest = cp.getConnection();
		return getSessions().openSession( connectionUnderTest );
	}

	protected void reconnect(Session session) {
		session.reconnect( connectionUnderTest );
	}

	protected void done() throws Throwable {
		cp.closeConnection( connectionUnderTest );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.ON_CLOSE.toString() );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, UserSuppliedConnectionProvider.class.getName() );
		boolean supportsScroll = true;
		Connection conn = null;
		try {
			conn = cp.getConnection();
			supportsScroll = conn.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
		}
		catch( Throwable ignore ) {
		}
		finally {
			if ( conn != null ) {
				try {
					conn.close();
				}
				catch( Throwable ignore ) {
					// ignore it...
				}
			}
		}
		cfg.setProperty( Environment.USE_SCROLLABLE_RESULTSET, "" + supportsScroll );
	}

	public boolean createSchema() {
		return false;
	}

	public boolean recreateSchemaAfterFailure() {
		return false;
	}

	protected void prepareTest() throws Exception {
		super.prepareTest();
		Connection conn = cp.getConnection();
		try {
			new SchemaExport( getCfg(), conn ).create( false, true );
		}
		finally {
			if ( conn != null ) {
				try {
					cp.closeConnection( conn );
				}
				catch( Throwable ignore ) {
				}
			}
		}
	}

	protected void cleanupTest() throws Exception {
		Connection conn = cp.getConnection();
		try {
			new SchemaExport( getCfg(), conn ).drop( false, true );
		}
		finally {
			if ( conn != null ) {
				try {
					cp.closeConnection( conn );
				}
				catch( Throwable ignore ) {
				}
			}
		}
		try {
			cp.close();
		}
		catch( Throwable ignore ) {
		}
		super.cleanupTest();
	}
}
