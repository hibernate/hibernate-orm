/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.ResourceClosedException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import static org.hibernate.ConnectionAcquisitionMode.IMMEDIATELY;
import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;
import static org.hibernate.ConnectionReleaseMode.BEFORE_TRANSACTION_COMPLETION;
import static org.hibernate.ConnectionReleaseMode.ON_CLOSE;
import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION;

/**
 * Represents a {@link LogicalConnection} where we manage obtaining and releasing the {@link Connection} as needed.
 * This implementation does not claim to be thread-safe and is not designed to be used by multiple
 * threads, yet we do apply a limited amount of care to be able to avoid obscure exceptions when
 * this class is used in the wrong way.
 *
 * @author Steve Ebersole
 */
public class LogicalConnectionManagedImpl extends AbstractLogicalConnectionImplementor {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( LogicalConnectionManagedImpl.class );

	private final transient JdbcSessionOwner jdbcSessionOwner;
	private final transient PhysicalConnectionHandlingMode connectionHandlingMode;

	private transient Connection physicalConnection;
	private boolean closed;

	public LogicalConnectionManagedImpl(JdbcSessionOwner sessionOwner, ResourceRegistry resourceRegistry) {
		this.jdbcSessionOwner = sessionOwner;
		this.resourceRegistry = resourceRegistry;

		connectionHandlingMode = determineConnectionHandlingMode( sessionOwner );
		if ( connectionHandlingMode.getAcquisitionMode() == IMMEDIATELY ) {
			//noinspection resource
			acquireConnectionIfNeeded();
		}

		if ( sessionOwner.getJdbcSessionContext().doesConnectionProviderDisableAutoCommit() ) {
			log.connectionProviderDisablesAutoCommitEnabled();
		}
	}

	private PhysicalConnectionHandlingMode determineConnectionHandlingMode(JdbcSessionOwner sessionOwner) {
		final var connectionHandlingMode = sessionOwner.getJdbcSessionContext().getPhysicalConnectionHandlingMode();
		return connectionHandlingMode.getReleaseMode() == AFTER_STATEMENT
			&& !sessionOwner.getJdbcConnectionAccess().supportsAggressiveRelease()
				? DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION
				: connectionHandlingMode;
	}

	private LogicalConnectionManagedImpl(JdbcSessionOwner owner, boolean closed) {
		this( owner, new ResourceRegistryStandardImpl() );
		this.closed = closed;
	}

	private Connection acquireConnectionIfNeeded() {
		if ( physicalConnection == null ) {
			final JdbcEventHandler eventHandler = jdbcSessionOwner.getJdbcSessionContext().getEventHandler();
			eventHandler.jdbcConnectionAcquisitionStart();
			try {
				physicalConnection = jdbcSessionOwner.getJdbcConnectionAccess().obtainConnection();
			}
			catch ( SQLException e ) {
				throw jdbcSessionOwner.getSqlExceptionHelper()
						.convert( e, "Unable to acquire JDBC Connection" );
			}
			finally {
				eventHandler.jdbcConnectionAcquisitionEnd( physicalConnection );
			}

			try {
				jdbcSessionOwner.afterObtainConnection( physicalConnection );
			}
			catch (SQLException e) {
				try {
					// given the session a chance to set the schema
					jdbcSessionOwner.getJdbcConnectionAccess().releaseConnection( physicalConnection );
				}
				catch (SQLException re) {
					e.addSuppressed( re );
				}
				throw jdbcSessionOwner.getSqlExceptionHelper()
						.convert( e, "Error after acquiring JDBC Connection" );
			}

		}

		return physicalConnection;
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	@Override
	public PhysicalConnectionHandlingMode getConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public boolean isPhysicallyConnected() {
		return physicalConnection != null;
	}

	@Override
	public Connection getPhysicalConnection() {
		errorIfClosed();
		return acquireConnectionIfNeeded();
	}

	@Override
	public void afterStatement() {
		super.afterStatement();
		if ( connectionHandlingMode.getReleaseMode() == AFTER_STATEMENT ) {
			if ( getResourceRegistry().hasRegisteredResources() ) {
				log.debug( "Skipping aggressive release of JDBC Connection after-statement due to held resources" );
			}
			else {
				log.debug( "Initiating JDBC connection release from afterStatement" );
				releaseConnection();
			}
		}
	}

	@Override
	public void beforeTransactionCompletion() {
		super.beforeTransactionCompletion();
		if ( connectionHandlingMode.getReleaseMode() == BEFORE_TRANSACTION_COMPLETION ) {
			log.debug( "Initiating JDBC connection release from beforeTransactionCompletion" );
			releaseConnection();
		}
	}

	@Override
	public void afterTransaction() {
		super.afterTransaction();
		if ( connectionHandlingMode.getReleaseMode() != ON_CLOSE ) {
			// NOTE: we check for !ON_CLOSE here (rather than AFTER_TRANSACTION) to also catch:
			// - AFTER_STATEMENT cases that were circumvented due to held resources
			// - BEFORE_TRANSACTION_COMPLETION cases that were circumvented because a rollback occurred
			//   (we don't get a beforeTransactionCompletion event on rollback).
			log.debug( "Initiating JDBC connection release from afterTransaction" );
			releaseConnection();
		}
	}

	@Override
	public Connection manualDisconnect() {
		if ( closed ) {
			throw new ResourceClosedException( "Logical connection is closed" );
		}
		final Connection connection = physicalConnection;
		releaseConnection();
		return connection;
	}

	@Override
	public void manualReconnect(Connection suppliedConnection) {
		if ( closed ) {
			throw new ResourceClosedException( "Logical connection is closed" );
		}
		throw new IllegalStateException( "Cannot manually reconnect unless Connection was originally supplied by user" );
	}

	private void releaseConnection() {
		final Connection localVariableConnection = physicalConnection;
		if ( localVariableConnection != null ) {
			try {
				// give the session a chance to change the schema back to null
				jdbcSessionOwner.beforeReleaseConnection( physicalConnection );
			}
			catch (SQLException e) {
				log.warn( "Error before releasing JDBC connection", e );
			}

			final JdbcEventHandler eventHandler = jdbcSessionOwner.getJdbcSessionContext().getEventHandler();
			// We need to set the connection to null before we release resources,
			// in order to prevent recursion into this method.
			// Recursion can happen when we release resources and when batch statements are in progress:
			// when releasing resources, we'll abort the batch statement,
			// which will trigger "logicalConnection.afterStatement()",
			// which in some configurations will release the connection.
			physicalConnection = null;
			try {
				try {
					getResourceRegistry().releaseResources();
					if ( !localVariableConnection.isClosed() ) {
						jdbcSessionOwner.getSqlExceptionHelper().logAndClearWarnings( localVariableConnection );
					}
				}
				finally {
					eventHandler.jdbcConnectionReleaseStart();
					jdbcSessionOwner.getJdbcConnectionAccess().releaseConnection( localVariableConnection );
				}
			}
			catch (SQLException e) {
				throw jdbcSessionOwner.getSqlExceptionHelper()
						.convert( e, "Unable to release JDBC Connection" );
			}
			finally {
				eventHandler.jdbcConnectionReleaseEnd();
			}
		}
	}

	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( closed );
	}

	public static LogicalConnectionManagedImpl deserialize(ObjectInputStream ois, JdbcSessionOwner owner)
			throws IOException {
		return new LogicalConnectionManagedImpl( owner, ois.readBoolean() );
	}

	@Override
	public Connection close() {
		if ( !closed ) {
			getResourceRegistry().releaseResources();
			log.closingLogicalConnection();
			try {
				releaseConnection();
			}
			finally {
				// no matter what
				closed = true;
				log.logicalConnectionClosed();
			}
		}
		return null;
	}


	// PhysicalJdbcTransaction impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	protected Connection getConnectionForTransactionManagement() {
		return getPhysicalConnection();
	}

	boolean initiallyAutoCommit;

	@Override
	public void begin() {
		initiallyAutoCommit = !doConnectionsFromProviderHaveAutoCommitDisabled()
				&& determineInitialAutoCommitMode( getConnectionForTransactionManagement() );
		super.begin();
	}

	@Override
	protected void afterCompletion() {
		resetConnection( initiallyAutoCommit );
		initiallyAutoCommit = false;
		afterTransaction();
	}

	@Override
	protected boolean doConnectionsFromProviderHaveAutoCommitDisabled() {
		return jdbcSessionOwner.getJdbcSessionContext().doesConnectionProviderDisableAutoCommit();
	}
}
