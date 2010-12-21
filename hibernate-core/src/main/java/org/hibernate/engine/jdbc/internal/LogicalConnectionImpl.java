/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.jdbc.BorrowedConnectionProxy;
import org.hibernate.stat.StatisticsImplementor;

/**
 * LogicalConnectionImpl implementation
 *
 * @author Steve Ebersole
 */
public class LogicalConnectionImpl implements LogicalConnectionImplementor {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                LogicalConnectionImpl.class.getPackage().getName());

	private Connection physicalConnection;
	private Connection borrowedConnection;

	private final ConnectionReleaseMode connectionReleaseMode;
	private final JdbcServices jdbcServices;
	private final StatisticsImplementor statisticsImplementor;
	private final JdbcResourceRegistry jdbcResourceRegistry;
	private final List<ConnectionObserver> observers = new ArrayList<ConnectionObserver>();

	private boolean releasesEnabled = true;

	private final boolean isUserSuppliedConnection;

	private boolean isClosed;

	public LogicalConnectionImpl(Connection userSuppliedConnection,
								 ConnectionReleaseMode connectionReleaseMode,
								 JdbcServices jdbcServices,
								 StatisticsImplementor statisticsImplementor
	) {
		this( connectionReleaseMode,
				jdbcServices,
				statisticsImplementor,
				userSuppliedConnection != null,
				false
		);
		this.physicalConnection = userSuppliedConnection;
	}

	private LogicalConnectionImpl(ConnectionReleaseMode connectionReleaseMode,
								  JdbcServices jdbcServices,
								  StatisticsImplementor statisticsImplementor,
								  boolean isUserSuppliedConnection,
								  boolean isClosed) {
		this.connectionReleaseMode = determineConnectionReleaseMode(
				jdbcServices, isUserSuppliedConnection, connectionReleaseMode
		);
		this.jdbcServices = jdbcServices;
		this.statisticsImplementor = statisticsImplementor;
		this.jdbcResourceRegistry =
				new JdbcResourceRegistryImpl( getJdbcServices().getSqlExceptionHelper() );

		this.isUserSuppliedConnection = isUserSuppliedConnection;
		this.isClosed = isClosed;
	}

	private static ConnectionReleaseMode determineConnectionReleaseMode(JdbcServices jdbcServices,
																		boolean isUserSuppliedConnection,
																		ConnectionReleaseMode connectionReleaseMode) {
		if ( isUserSuppliedConnection ) {
			return ConnectionReleaseMode.ON_CLOSE;
		}
		else if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
				! jdbcServices.getConnectionProvider().supportsAggressiveRelease() ) {
			log.debug( "connection provider reports to not support aggressive release; overriding" );
			return ConnectionReleaseMode.AFTER_TRANSACTION;
		}
		else {
			return connectionReleaseMode;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	/**
	 * {@inheritDoc}
	 */
	public StatisticsImplementor getStatisticsImplementor() {
		return statisticsImplementor;
	}

	/**
	 * {@inheritDoc}
	 */
	public JdbcResourceRegistry getResourceRegistry() {
		return jdbcResourceRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addObserver(ConnectionObserver observer) {
		observers.add( observer );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isOpen() {
		return !isClosed;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isLogicallyConnected() {
		return isUserSuppliedConnection ?
				isPhysicallyConnected() :
				isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isPhysicallyConnected() {
		return physicalConnection != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws HibernateException {
		if ( isClosed ) {
			throw new HibernateException( "Logical connection is closed" );
		}
		if ( physicalConnection == null ) {
			if ( isUserSuppliedConnection ) {
				// should never happen
				throw new HibernateException( "User-supplied connection was null" );
			}
			obtainConnection();
		}
		return physicalConnection;
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection close() {
		Connection c = physicalConnection;
		try {
			releaseBorrowedConnection();
			log.trace( "closing logical connection" );
			if ( !isUserSuppliedConnection && physicalConnection != null ) {
				jdbcResourceRegistry.close();
				releaseConnection();
			}
			return c;
		}
		finally {
			// no matter what
			physicalConnection = null;
			isClosed = true;
	        LOG.closedLogicalConnection();
			for ( ConnectionObserver observer : observers ) {
				observer.logicalConnectionClosed();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	public boolean hasBorrowedConnection() {
		return borrowedConnection != null;
	}

	public Connection borrowConnection() {
		if ( isClosed ) {
			throw new HibernateException( "connection has been closed" );
		}
		if ( isUserSuppliedConnection ) {
			return physicalConnection;
		}
		else {
			if ( borrowedConnection == null ) {
				borrowedConnection = BorrowedConnectionProxy.generateProxy( this );
			}
			return borrowedConnection;
		}
	}

	public void releaseBorrowedConnection() {
		if ( borrowedConnection != null ) {
			try {
				BorrowedConnectionProxy.renderUnuseable( borrowedConnection );
			}
			finally {
				borrowedConnection = null;
			}
		}
	}

	public void afterStatementExecution() {
        LOG.startingAfterStatementExecution(connectionReleaseMode);
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
                LOG.skippingAggressiveReleaseDueToManualDisabling();
				return;
			}
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
                LOG.skippingAggressiveReleaseDueToRegisteredResources();
				return;
			}
			else if ( borrowedConnection != null ) {
				log.debug( "skipping aggresive-release due to borrowed connection" );
			}
			releaseConnection();
		}
	}

	public void afterTransaction() {
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ||
				connectionReleaseMode == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
                LOG.forcingContainerResourceCleanup();
				jdbcResourceRegistry.releaseResources();
			}
			aggressiveRelease();
		}
	}

	public void disableReleases() {
        LOG.disablingReleases();
		releasesEnabled = false;
	}

	public void enableReleases() {
        LOG.enablingReleases();
		releasesEnabled = true;
		//FIXME: uncomment after new batch stuff is integrated!!!
		//afterStatementExecution();
	}

	/**
	 * Force aggresive release of the underlying connection.
	 */
	public void aggressiveRelease() {
		if ( isUserSuppliedConnection ) {
            LOG.unableToAggressivelyReleaseUserSuppliedConnection();
		}
		else {
			LOG.aggressivelyReleasingJdbcConnection(  );
			if ( physicalConnection != null ) {
				releaseConnection();
			}
		}
	}


	/**
	 * Pysically opens a JDBC Connection.
	 *
	 * @throws org.hibernate.JDBCException Indicates problem opening a connection
	 */
	private void obtainConnection() throws JDBCException {
        LOG.obtainingJdbcConnection();
		try {
			physicalConnection = getJdbcServices().getConnectionProvider().getConnection();
			for ( ConnectionObserver observer : observers ) {
				observer.physicalConnectionObtained( physicalConnection );
			}
            LOG.obtainedJdbcConnection();
		}
		catch ( SQLException sqle) {
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not open connection" );
		}
	}

	/**
	 * Physically closes the JDBC Connection.
	 *
	 * @throws JDBCException Indicates problem closing a connection
	 */
	private void releaseConnection() throws JDBCException {
        LOG.releasingJdbcConnection();
		if ( physicalConnection == null ) {
			return;
		}
		try {
			if ( ! physicalConnection.isClosed() ) {
				getJdbcServices().getSqlExceptionHelper().logAndClearWarnings( physicalConnection );
			}
			if ( !isUserSuppliedConnection ) {
				getJdbcServices().getConnectionProvider().closeConnection( physicalConnection );
			}
            LOG.releasedJdbcConnection();
		}
		catch (SQLException sqle) {
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not close connection" );
		}
		finally {
			physicalConnection = null;
		}
		log.debug( "released JDBC connection" );
		for ( ConnectionObserver observer : observers ) {
			observer.physicalConnectionReleased();
		}
	}

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection mantained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	public Connection manualDisconnect() {
		if ( isClosed ) {
			throw new IllegalStateException( "cannot manually disconnect because logical connection is already closed" );
		}
		Connection c = physicalConnection;
		jdbcResourceRegistry.releaseResources();
		releaseConnection();
		return c;
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for user-supplied connections.
	 */
	public void reconnect(Connection suppliedConnection) {
		if ( isClosed ) {
			throw new IllegalStateException( "cannot manually reconnect because logical connection is already closed" );
		}
		if ( isUserSuppliedConnection ) {
			if ( suppliedConnection == null ) {
				throw new IllegalArgumentException( "cannot reconnect a null user-supplied connection" );
			}
			else if ( suppliedConnection == physicalConnection ) {
				log.warn( "reconnecting the same connection that is already connected; should this connection have been disconnected?" );
			}
			else if ( physicalConnection != null ) {
				throw new IllegalArgumentException(
						"cannot reconnect to a new user-supplied connection because currently connected; must disconnect before reconnecting."
				);
			}
			physicalConnection = suppliedConnection;
			log.debug( "reconnected JDBC connection" );
		}
		else {
			if ( suppliedConnection != null ) {
				throw new IllegalStateException( "unexpected user-supplied connection" );
			}
			log.debug( "called reconnect() with null connection (not user-supplied)" );
		}
	}

	public boolean isReadyForSerialization() {
		return isUserSuppliedConnection ?
				! isPhysicallyConnected() :
				! getResourceRegistry().hasRegisteredResources()
				;
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( isUserSuppliedConnection );
		oos.writeBoolean( isClosed );
	}

	public static LogicalConnectionImpl deserialize(ObjectInputStream ois,
													JdbcServices jdbcServices,
													StatisticsImplementor statisticsImplementor,
													ConnectionReleaseMode connectionReleaseMode
	) throws IOException {
		return new LogicalConnectionImpl(
				connectionReleaseMode,
				jdbcServices,
				statisticsImplementor,
				ois.readBoolean(),
				ois.readBoolean()
		);
 	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    /*
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Aggressively releasing JDBC connection" )
        void aggressivelyReleasingJdbcConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Logical connection closed" )
        void closedLogicalConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Closing logical connection" )
        void closingLogicalConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Disabling releases" )
        void disablingReleases();

        @LogMessage( level = TRACE )
        @Message( value = "(Re)enabling releases" )
        void enablingReleases();

        @LogMessage( level = INFO )
        @Message( value = "Forcing container resource cleanup on transaction completion" )
        void forcingContainerResourceCleanup();

        @LogMessage( level = DEBUG )
        @Message( value = "Obtained JDBC connection" )
        void obtainedJdbcConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Obtaining JDBC connection" )
        void obtainingJdbcConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Released JDBC connection" )
        void releasedJdbcConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Releasing JDBC connection" )
        void releasingJdbcConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Skipping aggressive release due to manual disabling" )
        void skippingAggressiveReleaseDueToManualDisabling();

        @LogMessage( level = DEBUG )
        @Message( value = "Skipping aggressive release due to registered resources" )
        void skippingAggressiveReleaseDueToRegisteredResources();

        @LogMessage( level = TRACE )
        @Message( value = "Starting after statement execution processing [%s]" )
        void startingAfterStatementExecution( ConnectionReleaseMode connectionReleaseMode );

        @LogMessage( level = DEBUG )
        @Message( value = "Cannot aggressively release user-supplied connection; skipping" )
        void unableToAggressivelyReleaseUserSuppliedConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Connection provider reports to not support aggressive release; overriding" )
        void unsupportedAggressiveRelease();
    }
    */
}
