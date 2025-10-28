/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;


/**
 * A locking strategy where a lock is obtained via an update statement.
 * <p>
 * This strategy is not valid for read style locks.
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class UpdateLockingStrategy implements LockingStrategy {

	private final EntityPersister lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public UpdateLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.WRITE ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
		if ( !lockable.isVersioned() ) {
			CORE_LOGGER.writeLocksNotSupported( lockable.getEntityName() );
			this.sql = null;
		}
		else {
			this.sql = generateLockString();
		}
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			int timeout,
			SharedSessionContractImplementor session) throws StaleObjectStateException, JDBCException {
		final String lockableEntityName = lockable.getEntityName();
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockableEntityName + "]" );
		}

		// todo : should we additionally check the current isolation mode explicitly?
		final SessionFactoryImplementor factory = session.getFactory();
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			try {
				final BasicType<?> lockableVersionType = lockable.getVersionType();
				lockableVersionType.nullSafeSet( st, version, 1, session );
				int offset = 2;

				final Type lockableIdentifierType = lockable.getIdentifierType();
				lockableIdentifierType.nullSafeSet( st, id, offset, session );
				offset += lockableIdentifierType.getColumnSpan( factory.getRuntimeMetamodels() );

				if ( lockable.isVersioned() ) {
					lockableVersionType.nullSafeSet( st, version, offset, session );
				}

				final int affected = jdbcCoordinator.getResultSetReturn().executeUpdate( st, sql );
				if ( affected < 0 ) {
					final StatisticsImplementor statistics = factory.getStatistics();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.optimisticFailure( lockableEntityName );
					}
					throw new StaleObjectStateException( lockableEntityName, id );
				}

			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
			);
		}
	}

	protected String generateLockString() {
		final SessionFactoryImplementor factory = lockable.getFactory();
		final Update update = new Update( factory );
		update.setTableName( lockable.getRootTableName() );
		update.addAssignment( lockable.getVersionColumnName() );
		update.addRestriction( lockable.getRootTableIdentifierColumnNames() );
		update.addRestriction( lockable.getVersionColumnName() );
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return update.toStatementString();
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
