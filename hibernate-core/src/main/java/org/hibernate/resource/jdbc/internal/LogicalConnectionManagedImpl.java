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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
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

	private final transient JdbcConnectionAccess jdbcConnectionAccess;
	private final transient JdbcEventHandler jdbcEventHandler;
	private final transient SqlExceptionHelper sqlExceptionHelper;

	private final transient PhysicalConnectionHandlingMode connectionHandlingMode;

	private transient Connection physicalConnection;
	private boolean closed;

	private final boolean providerDisablesAutoCommit;

	public LogicalConnectionManagedImpl(
			JdbcConnectionAccess jdbcConnectionAccess,
			JdbcSessionContext jdbcSessionContext,
			SqlExceptionHelper sqlExceptionHelper,
			ResourceRegistry resourceRegistry) {
		this.jdbcConnectionAccess = jdbcConnectionAccess;
		this.sqlExceptionHelper = sqlExceptionHelper;
		this.resourceRegistry = resourceRegistry;
		jdbcEventHandler = jdbcSessionContext.getEventHandler();

		connectionHandlingMode = determineConnectionHandlingMode( jdbcSessionContext, jdbcConnectionAccess );
		if ( connectionHandlingMode.getAcquisitionMode() == IMMEDIATELY ) {
			//noinspection resource
			acquireConnectionIfNeeded();
		}

		providerDisablesAutoCommit = jdbcSessionContext.doesConnectionProviderDisableAutoCommit();
		if ( providerDisablesAutoCommit ) {
			log.connectionProviderDisablesAutoCommitEnabled();
		}
	}

	public LogicalConnectionManagedImpl(
			JdbcConnectionAccess jdbcConnectionAccess,
			JdbcSessionContext jdbcSessionContext,
			ResourceRegistry resourceRegistry,
			JdbcServices jdbcServices) {
		this(
				jdbcConnectionAccess,
				jdbcSessionContext,
				jdbcServices.getSqlExceptionHelper(),
				resourceRegistry
		);
	}

	public LogicalConnectionManagedImpl(JdbcSessionOwner owner, ResourceRegistry resourceRegistry) {
		this(
				owner.getJdbcConnectionAccess(),
				owner.getJdbcSessionContext(),
				owner.getSqlExceptionHelper(),
				resourceRegistry
		);
	}

	private PhysicalConnectionHandlingMode determineConnectionHandlingMode(
			JdbcSessionContext jdbcSessionContext,
			JdbcConnectionAccess jdbcConnectionAccess) {
		final var connectionHandlingMode = jdbcSessionContext.getPhysicalConnectionHandlingMode();
		return connectionHandlingMode.getReleaseMode() == AFTER_STATEMENT
			&& !jdbcConnectionAccess.supportsAggressiveRelease()
				? DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION
				: connectionHandlingMode;
	}

	private LogicalConnectionManagedImpl(
			JdbcConnectionAccess jdbcConnectionAccess,
			JdbcSessionContext jdbcSessionContext,
			boolean closed) {
		this(
				jdbcConnectionAccess,
				jdbcSessionContext,
				new ResourceRegistryStandardImpl(),
				jdbcSessionContext.getJdbcServices()
		);
		this.closed = closed;
	}

	private Connection acquireConnectionIfNeeded() {
		if ( physicalConnection == null ) {
			jdbcEventHandler.jdbcConnectionAcquisitionStart();
			try {
				physicalConnection = jdbcConnectionAccess.obtainConnection();
			}
			catch ( SQLException e ) {
				throw sqlExceptionHelper.convert( e, "Unable to acquire JDBC Connection" );
			}
			finally {
				jdbcEventHandler.jdbcConnectionAcquisitionEnd( physicalConnection );
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
						sqlExceptionHelper.logAndClearWarnings( localVariableConnection );
					}
				}
				finally {
					jdbcEventHandler.jdbcConnectionReleaseStart();
					jdbcConnectionAccess.releaseConnection( localVariableConnection );
				}
			}
			catch (SQLException e) {
				throw sqlExceptionHelper.convert( e, "Unable to release JDBC Connection" );
			}
			finally {
				jdbcEventHandler.jdbcConnectionReleaseEnd();
			}
		}
	}

	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( closed );
	}

	public static LogicalConnectionManagedImpl deserialize(
			ObjectInputStream ois,
			JdbcConnectionAccess jdbcConnectionAccess,
			JdbcSessionContext jdbcSessionContext)
			throws IOException {
		final boolean isClosed = ois.readBoolean();
		return new LogicalConnectionManagedImpl( jdbcConnectionAccess, jdbcSessionContext, isClosed );
	}

	public static LogicalConnectionManagedImpl deserialize(ObjectInputStream ois, JdbcSessionOwner owner)
			throws IOException {
		return deserialize( ois, owner.getJdbcConnectionAccess(), owner.getJdbcSessionContext() );
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
		return providerDisablesAutoCommit;
	}
}
