/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;

/**
 * A pessimistic locking strategy where the locks are obtained through select statements.
 * <p/>
 * For non-read locks, this is achieved through the Dialect's specific
 * SELECT ... FOR UPDATE syntax.
 *
 * This strategy is valid for LockMode.PESSIMISTIC_READ
 *
 * This class is a clone of SelectLockingStrategy.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(org.hibernate.LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(org.hibernate.LockMode, String)
 *
 * @since 3.5
 */
public class PessimisticReadSelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public PessimisticReadSelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		final String sql = determineSql( timeout );
		final SessionFactoryImplementor factory = session.getFactory();
		try {
			try {
				final PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
				try {
					getLockable().getIdentifierType().nullSafeSet( st, id, 1, session );
					if ( getLockable().isVersioned() ) {
						getLockable().getVersionType().nullSafeSet(
								st,
								version,
								getLockable().getIdentifierType().getColumnSpan( factory ) + 1,
								session
						);
					}

					final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( st );
					try {
						if ( !rs.next() ) {
							if ( factory.getStatistics().isStatisticsEnabled() ) {
								factory.getStatistics().optimisticFailure( getLockable().getEntityName() );
							}
							throw new StaleObjectStateException( getLockable().getEntityName(), id );
						}
					}
					finally {
						session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, st );
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
						"could not lock: " + MessageHelper.infoString( getLockable(), id, session.getFactory() ),
						sql
				);
			}
		}
		catch (JDBCException e) {
			throw new PessimisticEntityLockException( object, "could not obtain pessimistic lock", e );
		}
	}

	protected String generateLockString(int lockTimeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( lockTimeout );
		final SimpleSelect select = new SimpleSelect( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addCondition( getLockable().getRootTableIdentifierColumnNames(), "=?" );
		if ( getLockable().isVersioned() ) {
			select.addCondition( getLockable().getVersionColumnName(), "=?" );
		}
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
