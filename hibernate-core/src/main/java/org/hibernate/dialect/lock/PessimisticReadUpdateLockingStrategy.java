/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

/**
 * A pessimistic locking strategy where a lock is obtained via
 * an update statement.
 * <p>
 * This strategy is valid for {@link LockMode#PESSIMISTIC_READ}.
 * <p>
 * This class is a clone of {@link UpdateLockingStrategy}.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticReadUpdateLockingStrategy implements LockingStrategy {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			PessimisticReadUpdateLockingStrategy.class.getName()
	);

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
	public PessimisticReadUpdateLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
		if ( !lockable.isVersioned() ) {
			LOG.writeLocksNotSupported( lockable.getEntityName() );
			this.sql = null;
		}
		else {
			this.sql = generateLockString();
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}

		final SessionFactoryImplementor factory = session.getFactory();
		try {
			try {
				final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
				final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
				try {
					lockable.getVersionType().nullSafeSet( st, version, 1, session );
					int offset = 2;

					lockable.getIdentifierType().nullSafeSet( st, id, offset, session );
					offset += lockable.getIdentifierType().getColumnSpan( factory.getRuntimeMetamodels() );

					if ( lockable.isVersioned() ) {
						lockable.getVersionType().nullSafeSet( st, version, offset, session );
					}

					final int affected = jdbcCoordinator.getResultSetReturn().executeUpdate( st, sql );
					// todo:  should this instead check for exactly one row modified?
					if ( affected < 0 ) {
						final StatisticsImplementor statistics = factory.getStatistics();
						if ( statistics.isStatisticsEnabled() ) {
							statistics.optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}

				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
					jdbcCoordinator.afterStatementExecution();
				}

			}
			catch ( SQLException e ) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
						sql
				);
			}
		}
		catch (JDBCException e) {
			throw new PessimisticEntityLockException( object, "could not obtain pessimistic lock", e );
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
