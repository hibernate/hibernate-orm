/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;

import org.jboss.logging.Logger;

/**
 * A pessimistic locking strategy where the locks are obtained through update statements.
 * <p/>
 * This strategy is valid for LockMode.PESSIMISTIC_WRITE
 *
 * This class is a clone of UpdateLockingStrategy.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticWriteUpdateLockingStrategy implements LockingStrategy {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			PessimisticWriteUpdateLockingStrategy.class.getName()
	);

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.  Note that read-locks are not valid for this strategy.
	 */
	public PessimisticWriteUpdateLockingStrategy(Lockable lockable, LockMode lockMode) {
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
	public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}

		final SessionFactoryImplementor factory = session.getFactory();
		try {
			try {
				final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
				try {
					lockable.getVersionType().nullSafeSet( st, version, 1, session );
					int offset = 2;

					lockable.getIdentifierType().nullSafeSet( st, id, offset, session );
					offset += lockable.getIdentifierType().getColumnSpan( factory );

					if ( lockable.isVersioned() ) {
						lockable.getVersionType().nullSafeSet( st, version, offset, session );
					}

					final int affected = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st );
					// todo:  should this instead check for exactly one row modified?
					if ( affected < 0 ) {
						if (factory.getStatistics().isStatisticsEnabled()) {
							factory.getStatistics().optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}

				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
					session.getJdbcCoordinator().afterStatementExecution();
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
		final Update update = new Update( factory.getDialect() );
		update.setTableName( lockable.getRootTableName() );
		update.addPrimaryKeyColumns( lockable.getRootTableIdentifierColumnNames() );
		update.setVersionColumnName( lockable.getVersionColumnName() );
		update.addColumn( lockable.getVersionColumnName() );
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return update.toStatementString();
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
