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
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import static org.hibernate.ConnectionAcquisitionMode.IMMEDIATELY;
import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;
import static org.hibernate.ConnectionReleaseMode.BEFORE_TRANSACTION_COMPLETION;
import static org.hibernate.ConnectionReleaseMode.ON_CLOSE;
import static org.hibernate.resource.jdbc.internal.LogicalConnectionLogging.CONNECTION_LOGGER;
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
			CONNECTION_LOGGER.connectionProviderDisablesAutoCommitEnabled();
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

	private JdbcSessionContext getJdbcSessionContext() {
		return jdbcSessionOwner.getJdbcSessionContext();
	}

	private JdbcConnectionAccess getJdbcConnectionAccess() {
		return jdbcSessionOwner.getJdbcConnectionAccess();
	}

	private SqlExceptionHelper getExceptionHelper() {
		return jdbcSessionOwner.getSqlExceptionHelper();
	}

	private Connection acquireConnectionIfNeeded() {
		if ( physicalConnection == null ) {
			physicalConnection = acquire();
			afterAcquire();
		}
		return physicalConnection;
	}

	private void releaseConnectionIfNeeded() {
		final Connection connection = physicalConnection;
		if ( connection != null ) {
			beforeRelease();
			// Set the connection to null before releasing resources to prevent
			// recursion into this method. Recursion can happen when we release
			// resources and when batch statements are in progress: releasing
			// resources aborts the batch statement, which then triggers
			// logicalConnection.afterStatement(), which in some configurations
			// releases the connection.
			physicalConnection = null;
			release( connection );
		}
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
	CONNECTION_LOGGER.skipConnectionReleaseAfterStatementDueToResources( hashCode() );
			}
			else {
	CONNECTION_LOGGER.initiatingConnectionReleaseAfterStatement( hashCode() );
				releaseConnectionIfNeeded();
			}
		}
	}

	@Override
	public void beforeTransactionCompletion() {
		super.beforeTransactionCompletion();
		if ( connectionHandlingMode.getReleaseMode() == BEFORE_TRANSACTION_COMPLETION ) {
CONNECTION_LOGGER.initiatingConnectionReleaseBeforeTransactionCompletion( hashCode() );
			releaseConnectionIfNeeded();
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
CONNECTION_LOGGER.initiatingConnectionReleaseAfterTransaction( hashCode() );
			releaseConnectionIfNeeded();
		}
	}

	@Override
	public Connection manualDisconnect() {
		if ( closed ) {
			throw new ResourceClosedException( "Logical connection is closed" );
		}
		final Connection connection = physicalConnection;
		releaseConnectionIfNeeded();
		return connection;
	}

	@Override
	public void manualReconnect(Connection suppliedConnection) {
		if ( closed ) {
			throw new ResourceClosedException( "Logical connection is closed" );
		}
		throw new IllegalStateException( "Cannot manually reconnect unless Connection was originally supplied by user" );
	}

	private Connection acquire() {
		final var eventHandler = getJdbcSessionContext().getEventHandler();
		eventHandler.jdbcConnectionAcquisitionStart();
		try {
			return getJdbcConnectionAccess().obtainConnection();
		}
		catch ( SQLException e ) {
			throw getExceptionHelper().convert( e, "Unable to acquire JDBC Connection" );
		}
		finally {
			eventHandler.jdbcConnectionAcquisitionEnd( physicalConnection );
		}
	}

	private void release(Connection connection) {
		final var eventHandler = getJdbcSessionContext().getEventHandler();
		try {
			try {
				getResourceRegistry().releaseResources();
				if ( !connection.isClosed() ) {
					getExceptionHelper().logAndClearWarnings( connection );
				}
			}
			finally {
				eventHandler.jdbcConnectionReleaseStart();
				getJdbcConnectionAccess().releaseConnection( connection );
			}
		}
		catch (SQLException e) {
			throw getExceptionHelper().convert( e, "Unable to release JDBC Connection" );
		}
		finally {
			eventHandler.jdbcConnectionReleaseEnd();
		}
	}

	private void afterAcquire() {
		try {
			// give the session a chance to set the schema
			jdbcSessionOwner.afterObtainConnection( physicalConnection );
		}
		catch (SQLException e) {
			try {
				getJdbcConnectionAccess().releaseConnection( physicalConnection );
			}
			catch (SQLException re) {
				e.addSuppressed( re );
			}
			throw getExceptionHelper().convert( e, "Error after acquiring JDBC Connection" );
		}
	}

	private void beforeRelease() {
		try {
			// give the session a chance to change the schema back to null
			jdbcSessionOwner.beforeReleaseConnection( physicalConnection );
		}
		catch (SQLException e) {
CONNECTION_LOGGER.errorBeforeReleasingJdbcConnection( hashCode(), e );
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
CONNECTION_LOGGER.closingLogicalConnection( hashCode() );
			try {
				releaseConnectionIfNeeded();
			}
			finally {
				// no matter what
				closed = true;
	CONNECTION_LOGGER.logicalConnectionClosed( hashCode() );
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
		initiallyAutoCommit =
				!doConnectionsFromProviderHaveAutoCommitDisabled()
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
		return getJdbcSessionContext().doesConnectionProviderDisableAutoCommit();
	}
}
