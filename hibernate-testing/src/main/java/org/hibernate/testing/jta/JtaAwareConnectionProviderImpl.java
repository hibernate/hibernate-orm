/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;

/**
 * A {@link ConnectionProvider} implementation intended for testing Hibernate/JTA interaction.  In that limited scope we
 * only ever have one single resource (the database connection) so we do not at all care about full-blown XA
 * semantics.  This class behaves accordingly.  This class also assumes usage of and access to JBossTS/Arjuna.
 *
 * @author Steve Ebersole
 * @author Jonathan Halliday
 */
public class JtaAwareConnectionProviderImpl implements ConnectionProvider, Configurable, Stoppable,
		ServiceRegistryAwareService {
	private static final String CONNECTION_KEY = "_database_connection";

	private final SharedDriverManagerConnectionProviderImpl delegate = SharedDriverManagerConnectionProviderImpl.getInstance();

	private final List<Connection> nonEnlistedConnections = new ArrayList<>();

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		delegate.injectServices( serviceRegistry );
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		Map<String,Object> connectionSettings = new HashMap<>();
		transferSetting( Environment.DRIVER, configurationValues, connectionSettings );
		transferSetting( Environment.URL, configurationValues, connectionSettings );
		transferSetting( Environment.USER, configurationValues, connectionSettings );
		transferSetting( Environment.PASS, configurationValues, connectionSettings );
		transferSetting( Environment.ISOLATION, configurationValues, connectionSettings );
		Properties passThroughSettings = ConnectionProviderInitiator.getConnectionProperties( configurationValues );
		for ( String setting : passThroughSettings.stringPropertyNames() ) {
			transferSetting( Environment.CONNECTION_PREFIX + '.' + setting, configurationValues, connectionSettings );
		}
		// We don't need this setting for configuring the connection provider
		connectionSettings.remove( AvailableSettings.CONNECTION_HANDLING );

		connectionSettings.put( Environment.AUTOCOMMIT, "false" );
		connectionSettings.put( Environment.POOL_SIZE, "5" );
		connectionSettings.put( DriverManagerConnectionProviderImpl.INITIAL_SIZE, "0" );

		delegate.configure( connectionSettings );
	}

	private static void transferSetting(String settingName, Map<String,Object> source, Map<String,Object> target) {
		Object value = source.get( settingName );
		if ( value != null ) {
			target.put( settingName, value );
		}
	}

	@Override
	public void stop() {
		delegate.stop();
	}

	@Override
	public Connection getConnection() throws SQLException {
		Transaction currentTransaction = findCurrentTransaction();

		try {
			if ( currentTransaction == null ) {
				// this block handles non enlisted connections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				Connection connection = delegate.getConnection();
				nonEnlistedConnections.add( connection );
				return connection;
			}

			// this portion handles enlisted connections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			Connection connection = (Connection) TestingJtaPlatformImpl.synchronizationRegistry().getResource(
					CONNECTION_KEY
			);
			if ( connection == null ) {
				connection = delegate.getConnection();
				TestingJtaPlatformImpl.synchronizationRegistry().putResource( CONNECTION_KEY, connection );
				try {
					XAResourceWrapper xaResourceWrapper = new XAResourceWrapper( this, connection );
					currentTransaction.enlistResource( xaResourceWrapper );
				}
				catch (Exception e) {
					delist( connection );
					throw e;
				}
			}
			return connection;
		}
		catch (SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new SQLException(e);
		}
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		if ( conn == null ) {
			return;
		}

		if ( nonEnlistedConnections.contains( conn ) ) {
			nonEnlistedConnections.remove( conn );
			delegate.closeConnection( conn );
		}

//		else {
			// do nothing.  part of the enlistment contract here is that the XAResource wrapper
			// takes that responsibility.
//		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}

	protected Transaction findCurrentTransaction() {
		try {
			return TestingJtaPlatformImpl.transactionManager().getTransaction();
		}
		catch (SystemException e) {
			throw new IllegalStateException( "Could not locate current transaction" );
		}
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return delegate.isUnwrappableAs( unwrapType );
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return delegate.unwrap( unwrapType );
	}

	@AllowSysOut
	private void delist(Connection connection) {
		// todo : verify the incoming connection is the currently enlisted one?
		try {
			TestingJtaPlatformImpl.synchronizationRegistry().putResource( CONNECTION_KEY, null );
		}
		catch ( Exception e ) {
			System.err.println( "!!!Error trying to reset synchronization registry!!!" );
		}
		try {
			delegate.closeConnection( connection );
		}
		catch (SQLException e) {
			System.err.println( "!!!Error trying to close JDBC connection from delist callbacks!!!" );
		}
	}

	public static class XAResourceWrapper implements XAResource {
		private final JtaAwareConnectionProviderImpl pool;
		private final Connection connection;
		private int transactionTimeout;

		public XAResourceWrapper(JtaAwareConnectionProviderImpl pool, Connection connection) {
			this.pool = pool;
			this.connection = connection;
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			throw new RuntimeException("this should never be called");
		}

		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			if (!onePhase) {
				throw new IllegalArgumentException( "must be one phase" );
			}

			try {
				connection.commit();
			}
			catch(SQLException e) {
				throw new XAException( e.toString() );
			}
			finally {
				try {
					pool.delist( connection );
				}
				catch (Exception ignore) {
				}
			}
		}

		@Override
		public void rollback(Xid xid) throws XAException {

			try {
				connection.rollback();
			}
			catch(SQLException e) {
				throw new XAException( e.toString() );
			}
			finally {
				try {
					pool.delist( connection );
				}
				catch (Exception ignore) {
				}
			}
		}

		@Override
		public void end(Xid xid, int i) throws XAException {
			// noop
		}

		@Override
		public void start(Xid xid, int i) throws XAException {
			// noop
		}


		@Override
		public void forget(Xid xid) throws XAException {
			// noop
		}

		@Override
		public int getTransactionTimeout() {
			return transactionTimeout;
		}

		@Override
		public boolean setTransactionTimeout(int i) {
			transactionTimeout = i;
			return true;
		}

		@Override
		public boolean isSameRM(XAResource xaResource) {
			return xaResource == this;
		}

		@Override
		public Xid[] recover(int i) {
			return new Xid[0];
		}
	}
}
