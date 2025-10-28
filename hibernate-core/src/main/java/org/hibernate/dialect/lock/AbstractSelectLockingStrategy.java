/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.SimpleSelect;

import java.sql.SQLException;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Base {@link LockingStrategy} implementation to support implementations
 * based on issuing SQL {@code SELECT} statements. For non-read locks,
 * this is achieved via the dialect's native {@code SELECT ... FOR UPDATE}
 * syntax.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectLockingStrategy implements LockingStrategy {
	private final EntityPersister lockable;
	private final LockMode lockMode;
	private final String waitForeverSql;

	protected AbstractSelectLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.waitForeverSql = generateLockString( WAIT_FOREVER );
	}

	protected EntityPersister getLockable() {
		return lockable;
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected String generateLockString(Timeout lockTimeout) {
		// for now, use the deprecated form passing the milliseconds to avoid copy/paste.
		// move that logic here when we can remove that overload.
		return generateLockString( lockTimeout.milliseconds() );
	}

	/**
	 * @deprecated Use {@linkplain #generateLockString(Timeout)} instead.
	 */
	@Deprecated
	protected String generateLockString(int lockTimeout) {
		final var factory = lockable.getFactory();
		final var lockOptions = new LockOptions( lockMode );
		lockOptions.setTimeOut( lockTimeout );
		final var select =
				new SimpleSelect( factory )
						.setLockOptions( lockOptions )
						.setTableName( lockable.getRootTableName() )
						.addColumn( lockable.getRootTableIdentifierColumnNames()[0] )
						.addRestriction( lockable.getRootTableIdentifierColumnNames() );
		if ( lockable.isVersioned() ) {
			select.addRestriction( lockable.getVersionColumnName() );
		}
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return select.toStatementString();
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, JDBCException {
		final String sql = determineSql( timeout );
		final var factory = session.getFactory();
		final var lockable = getLockable();
		try {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			try {
				lockable.getIdentifierType().nullSafeSet( preparedStatement, id, 1, session );
				if ( lockable.isVersioned() ) {
					lockable.getVersionType().nullSafeSet(
							preparedStatement,
							version,
							lockable.getIdentifierType().getColumnSpan( factory.getRuntimeMetamodels() ) + 1,
							session
					);
				}

				final var resultSet = jdbcCoordinator.getResultSetReturn().extract( preparedStatement, sql );
				try {
					if ( !resultSet.next() ) {
						final var statistics = factory.getStatistics();
						if ( statistics.isStatisticsEnabled() ) {
							statistics.optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( resultSet, preparedStatement );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException sqle ) {
			throw convertException( object, jdbcException( id, session, sqle, sql ) );
		}
	}

	private JDBCException jdbcException(Object id, SharedSessionContractImplementor session, SQLException sqle, String sql) {
		return session.getJdbcServices().getSqlExceptionHelper()
				.convert( sqle, "could not lock: " + infoString( lockable, id, session.getFactory() ), sql );
	}

	protected HibernateException convertException(Object entity, JDBCException ex) {
		return ex;
	}

	protected String determineSql(int timeout) {
		return switch ( timeout ) {
			case WAIT_FOREVER_MILLI -> waitForeverSql;
			case NO_WAIT_MILLI -> getNoWaitSql();
			case SKIP_LOCKED_MILLI -> getSkipLockedSql();
			default -> generateLockString( timeout );
		};
	}

	private String noWaitSql;

	protected String getNoWaitSql() {
		if ( noWaitSql == null ) {
			noWaitSql = generateLockString( NO_WAIT_MILLI );
		}
		return noWaitSql;
	}

	private String skipLockedSql;

	protected String getSkipLockedSql() {
		if ( skipLockedSql == null ) {
			skipLockedSql = generateLockString( SKIP_LOCKED_MILLI );
		}
		return skipLockedSql;
	}
}
