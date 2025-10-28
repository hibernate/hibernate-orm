/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;

import java.sql.SQLException;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * Common implementation of {@link PessimisticReadUpdateLockingStrategy}
 * and {@link PessimisticWriteUpdateLockingStrategy}.
 *
 * @since 7.2
 */
public abstract class AbstractPessimisticUpdateLockingStrategy implements LockingStrategy {

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
	public AbstractPessimisticUpdateLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( !lockable.isVersioned() ) {
			CORE_LOGGER.writeLocksNotSupported( lockable.getEntityName() );
			this.sql = null;
		}
		else {
			this.sql = generateLockString();
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		try {
			doLock( id, version, session );
		}
		catch (JDBCException e) {
			throw new PessimisticEntityLockException( object, "could not obtain pessimistic lock", e );
		}
	}

	void doLock(Object id, Object version, SharedSessionContractImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		try {
			final var factory = session.getFactory();
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var preparedStatement = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
			try {
				final var versionType = lockable.getVersionType();
				final var identifierType = lockable.getIdentifierType();

				versionType.nullSafeSet( preparedStatement, version, 1, session );
				int offset = 2;

				identifierType.nullSafeSet( preparedStatement, id, offset, session );
				offset += identifierType.getColumnSpan( factory.getRuntimeMetamodels() );

				if ( lockable.isVersioned() ) {
					versionType.nullSafeSet( preparedStatement, version, offset, session );
				}

				final int affected = jdbcCoordinator.getResultSetReturn().executeUpdate( preparedStatement, sql );
				// todo:  should this instead check for exactly one row modified?
				if ( affected < 0 ) {
					final var statistics = factory.getStatistics();
					final String entityName = lockable.getEntityName();
					if ( statistics.isStatisticsEnabled() ) {
						statistics.optimisticFailure( entityName );
					}
					throw new StaleObjectStateException( entityName, id );
				}

			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( preparedStatement );
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

	protected String generateLockString() {
		final var factory = lockable.getFactory();
		final var update = new Update( factory );
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
