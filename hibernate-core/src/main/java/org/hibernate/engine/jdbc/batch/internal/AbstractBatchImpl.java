/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.jdbc.ResourceRegistry;

/**
 * Convenience base class for implementers of the Batch interface.
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractBatchImpl implements Batch {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AbstractBatchImpl.class.getName()
	);

	private final BatchKey key;
	private final JdbcCoordinator jdbcCoordinator;

	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;

	private LinkedHashMap<String, PreparedStatement> statements = new LinkedHashMap<>();
	private LinkedHashSet<BatchObserver> observers = new LinkedHashSet<>();

	protected AbstractBatchImpl(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		if ( key == null ) {
			throw new IllegalArgumentException( "batch key cannot be null" );
		}
		if ( jdbcCoordinator == null ) {
			throw new IllegalArgumentException( "JDBC coordinator cannot be null" );
		}
		this.key = key;
		this.jdbcCoordinator = jdbcCoordinator;

		final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry()
				.getService( JdbcServices.class );

		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
	}

	protected JdbcCoordinator getJdbcCoordinator(){
		return this.jdbcCoordinator;
	}

	/**
	 * Perform batch execution..
	 * <p/>
	 * This is called from the explicit {@link #execute() execution}, but may also be called from elsewhere
	 * depending on the exact implementation.
	 */
	protected abstract void doExecuteBatch();

	/**
	 * Convenience access to the SQLException helper.
	 *
	 * @return The underlying SQLException helper.
	 */
	protected SqlExceptionHelper sqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	/**
	 * Convenience access to the SQL statement logger.
	 *
	 * @return The underlying JDBC services.
	 */
	protected SqlStatementLogger sqlStatementLogger() {
		return sqlStatementLogger;
	}

	protected void abortBatch() {
		jdbcCoordinator.abortBatch();
	}

	/**
	 * Access to the batch's map of statements (keyed by SQL statement string).
	 *
	 * @return This batch's statements.
	 */
	protected LinkedHashMap<String,PreparedStatement> getStatements() {
		return statements;
	}

	@Override
	public final BatchKey getKey() {
		return key;
	}

	@Override
	public void addObserver(BatchObserver observer) {
		observers.add( observer );
	}

	@Override
	public PreparedStatement getBatchStatement(String sql, boolean callable) {
		if ( sql == null ) {
			throw new IllegalArgumentException( "sql must be non-null." );
		}
		PreparedStatement statement = statements.get( sql );
		if ( statement == null ) {
			statement = buildBatchStatement( sql, callable );
			statements.put( sql, statement );
		}
		else {
			LOG.debug( "Reusing batch statement" );
			sqlStatementLogger().logStatement( sql );
		}
		return statement;
	}

	private PreparedStatement buildBatchStatement(String sql, boolean callable) {
		return jdbcCoordinator.getStatementPreparer().prepareStatement( sql, callable );
	}

	@Override
	public final void execute() {
		notifyObserversExplicitExecution();
		if ( getStatements().isEmpty() ) {
			return;
		}

		try {
			doExecuteBatch();
		}
		finally {
			releaseStatements();
		}
	}

	protected void releaseStatements() {
		final LinkedHashMap<String, PreparedStatement> statements = getStatements();
		final ResourceRegistry resourceRegistry = jdbcCoordinator.getResourceRegistry();
		for ( PreparedStatement statement : statements.values() ) {
			clearBatch( statement );
			resourceRegistry.release( statement );
		}
		// IMPL NOTE: If the statements are not cleared and JTA is being used, then
		//            jdbcCoordinator.afterStatementExecution() will abort the batch and a
		//            warning will be logged. To avoid the warning, clear statements first,
		//            before calling jdbcCoordinator.afterStatementExecution().
		statements.clear();
		jdbcCoordinator.afterStatementExecution();
	}

	protected void clearBatch(PreparedStatement statement) {
		try {
			// This code can be called after the connection is released
			// and the statement is closed. If the statement is closed,
			// then SQLException will be thrown when PreparedStatement#clearBatch
			// is called.
			// Ensure the statement is not closed before
			// calling PreparedStatement#clearBatch.
			if ( !statement.isClosed() ) {
				statement.clearBatch();
			}
		}
		catch ( SQLException e ) {
			LOG.unableToReleaseBatchStatement();
		}
	}

	/**
	 * Convenience method to notify registered observers of an explicit execution of this batch.
	 */
	protected final void notifyObserversExplicitExecution() {
		for ( BatchObserver observer : observers ) {
			observer.batchExplicitlyExecuted();
		}
	}

	/**
	 * Convenience method to notify registered observers of an implicit execution of this batch.
	 */
	protected final void notifyObserversImplicitExecution() {
		for ( BatchObserver observer : observers ) {
			observer.batchImplicitlyExecuted();
		}
	}

	@Override
	public void release() {
		if ( getStatements() != null && !getStatements().isEmpty() ) {
			LOG.batchContainedStatementsOnRelease();
		}
		releaseStatements();
		observers.clear();
	}
}
