/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

/**
 * {@link ConnectionProvider} delegating work to a real connection provider
 * and executing some operations when the first connection request returns a valid
 * connection.
 *
 * Useful to make sure an application can start even if the database is nto yet ready or present
 * while still be able to execute schema generation orders.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class DelayerConnectionProvider implements
		ConnectionProvider,
		Configurable,
		ServiceRegistryAwareService,
		Stoppable {

	private static final Logger LOGGER = Logger.getLogger( DelayerConnectionProvider.class );

	private final ConnectionProvider connectionProvider;
	private ServiceRegistry serviceRegistry;
	// volatile due to the double checked lock
	private volatile List<Runnable> operations = new ArrayList<>();
	private final ReentrantLock lock = new ReentrantLock();

	public DelayerConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * Operations can only be added before the {@code SessionFactory} is fully built.
	 * This is to prevent concurrent additions of operations.
	 * The operation must use the {@link ServiceRegistry} provided to retrieve
	 * {@link ConnectionProvider} to get a connection.
	 *
	 * The operation will try and be executed eagerly, we might be lucky.
	 *
	 * @param operation on a connection ready database
	 */
	public void addOperation(Runnable operation) {
		operations.add( operation );
		tryAndExecuteOperations();
	}

	/**
	 * Try to eagerly flush the operations if a connection is available
	 */
	private void tryAndExecuteOperations() {
		// It would be more elegant to refactor the operation lock from getConnection is a shared method
		// but I prefer the JVM to have more chances to keep getConnection() inlined
		try {
			Connection c = getConnection();
			closeConnection( c );
			if (operations == null) {
				// operation execution has succeeded, but subsequent ones might happen
				// remember, we are not multi-threaded here as we are bootstrapping Hibernate ORM
				operations = new ArrayList<>();
			}
		}
		catch (Exception e) {
			// we don't care if there is a failure
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		if ( connectionProvider instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) connectionProvider ).injectServices( serviceRegistry );
		}
	}

	@Override
	public void configure(Map configurationValues) {
		if ( connectionProvider instanceof Configurable ) {
			Configurable configurableConnectionProvider = (Configurable) connectionProvider;
			configurableConnectionProvider.configure( configurationValues );
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection;
		try {
			connection = connectionProvider.getConnection();
		}
		catch (Exception e) {
			throw e;
		}

		// protect operation execution behind a double checked lock
		// we don't want them to be applied multiple times
		if ( operations != null ) {
			// if lock is already held, we are reentering and acquiring a connection for the an operation execution
			// so we need to ignore operations here
			if ( ! lock.isHeldByCurrentThread() ) {
				lock.lock();
				try {
					if ( operations != null ) {
						// get a service registry bypassing the DelayerConnectionProvider for the action calls
						//ServiceRegistry sr = buildServiceRegistryDelegator();
						operations.forEach( Runnable::run );
						// if an operation fails, the list is not cleared but operations are
						// catastrophic events and further use should not happen
						operations = null;
					}
				}
				finally {
					lock.unlock();
				}
			}
		}

		return connection;
	}

	private ServiceRegistry buildServiceRegistryDelegator() {
		return new ServiceRegistry() {
			@Override
			public ServiceRegistry getParentServiceRegistry() {
				return DelayerConnectionProvider.this.serviceRegistry.getParentServiceRegistry();
			}

			@Override
			public <R extends Service> R getService(Class<R> serviceRole) {
				if ( ConnectionProvider.class.equals( serviceRole ) ) {
					return (R) DelayerConnectionProvider.this.connectionProvider;
				}
				return DelayerConnectionProvider.this.serviceRegistry.getService(serviceRole);
			}
		};
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		connectionProvider.closeConnection( conn );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		if ( DelayerConnectionProvider.class.equals( unwrapType ) ) {
			return true;
		}
		return connectionProvider.isUnwrappableAs( unwrapType );
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		if ( DelayerConnectionProvider.class.equals( unwrapType ) ) {
			return (T) this;
		}
		return connectionProvider.unwrap( unwrapType );
	}

	@Override
	public void stop() {
		if ( connectionProvider instanceof Stoppable ) {
			( (Stoppable) connectionProvider ).stop();
		}
	}
}
