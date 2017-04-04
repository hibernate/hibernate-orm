/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;

import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class LogicalConnectionProvidedImpl extends AbstractLogicalConnectionImplementor {
	private static final Logger log = Logger.getLogger( LogicalConnection.class );

	private transient Connection providedConnection;
	private final boolean initiallyAutoCommit;
	private boolean closed;

	public LogicalConnectionProvidedImpl(Connection providedConnection) {
		this( providedConnection, new ResourceRegistryStandardImpl() );
	}

	public LogicalConnectionProvidedImpl(Connection providedConnection, ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
		if ( providedConnection == null ) {
			throw new IllegalArgumentException( "Provided Connection cannot be null" );
		}

		this.providedConnection = providedConnection;
		this.initiallyAutoCommit = determineInitialAutoCommitMode( providedConnection );
	}

	private LogicalConnectionProvidedImpl(boolean closed, boolean initiallyAutoCommit) {
		this.resourceRegistry = new ResourceRegistryStandardImpl();
		this.closed = closed;
		this.initiallyAutoCommit = initiallyAutoCommit;
	}

	@Override
	public PhysicalConnectionHandlingMode getConnectionHandlingMode() {
		return PhysicalConnectionHandlingMode.IMMEDIATE_ACQUISITION_AND_HOLD;
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	@Override
	public Connection close() {
		log.trace( "Closing logical connection" );

		getResourceRegistry().releaseResources();

		try {
			return providedConnection;
		}
		finally {
			providedConnection = null;
			closed = true;
			log.trace( "Logical connection closed" );
		}
	}

	@Override
	public boolean isPhysicallyConnected() {
		return providedConnection != null;
	}

	@Override
	public Connection getPhysicalConnection() {
		errorIfClosed();
		return providedConnection;
	}

	@Override
	public LogicalConnectionImplementor makeShareableCopy() {
		errorIfClosed();

		return new LogicalConnectionProvidedImpl( providedConnection );
	}

	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( closed );
		oos.writeBoolean( initiallyAutoCommit );
	}

	public static LogicalConnectionProvidedImpl deserialize(
			ObjectInputStream ois) throws IOException, ClassNotFoundException {
		final boolean isClosed = ois.readBoolean();
		final boolean initiallyAutoCommit = ois.readBoolean();
		return new LogicalConnectionProvidedImpl( isClosed, initiallyAutoCommit );
	}

	@Override
	public Connection manualDisconnect() {
		errorIfClosed();
		try {
			resourceRegistry.releaseResources();
			return providedConnection;
		}
		finally {
			this.providedConnection = null;
		}
	}

	@Override
	public void manualReconnect(Connection connection) {
		errorIfClosed();

		if ( connection == null ) {
			throw new IllegalArgumentException( "cannot reconnect using a null connection" );
		}
		else if ( connection == providedConnection ) {
			// likely an unmatched reconnect call (no matching disconnect call)
			log.debug( "reconnecting the same connection that is already connected; should this connection have been disconnected?" );
		}
		else if ( providedConnection != null ) {
			throw new IllegalArgumentException(
					"cannot reconnect to a new user-supplied connection because currently connected; must disconnect beforeQuery reconnecting."
			);
		}
		providedConnection = connection;
		log.debug( "Manually reconnected logical connection" );
	}

	@Override
	protected Connection getConnectionForTransactionManagement() {
		return providedConnection;
	}

	@Override
	protected void afterCompletion() {
		afterTransaction();

		resetConnection( initiallyAutoCommit );
	}
}
