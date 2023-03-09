/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * A pessimistic locking strategy where a lock is obtained via a
 * select statements.
 * <p>
 * For non-read locks, this is achieved through the dialect's native
 * {@code SELECT ... FOR UPDATE} syntax.
 * <p>
 * This strategy is valid for {@link LockMode#PESSIMISTIC_READ}.
 * <p>
 * This class is a clone of {@link SelectLockingStrategy}.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(LockOptions, String)
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
	public void lock(Object id, Object version, Object object, int timeout, EventSource session) {
		final String sql = determineSql( timeout );
		final SessionFactoryImplementor factory = session.getFactory();
		try {
			final Lockable lockable = getLockable();
			try {
				final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
				final PreparedStatement st = jdbcCoordinator.getStatementPreparer().prepareStatement( sql );
				try {
					lockable.getIdentifierType().nullSafeSet( st, id, 1, session );
					if ( lockable.isVersioned() ) {
						lockable.getVersionType().nullSafeSet(
								st,
								version,
								lockable.getIdentifierType().getColumnSpan( factory ) + 1,
								session
						);
					}

					final ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st );
					if ( !rs.next() ) {
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

	protected String generateLockString(int lockTimeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( lockTimeout );
		final SimpleSelect select = new SimpleSelect( factory )
				.setLockOptions( lockOptions )
				.setTableName( getLockable().getRootTableName() )
				.addColumn( getLockable().getRootTableIdentifierColumnNames()[0] )
				.addRestriction( getLockable().getRootTableIdentifierColumnNames() );
		if ( getLockable().isVersioned() ) {
			select.addRestriction( getLockable().getVersionColumnName() );
		}
		if ( factory.getSessionFactoryOptions().isCommentsEnabled() ) {
			select.setComment( getLockMode() + " lock " + getLockable().getEntityName() );
		}
		return select.toStatementString();
	}
}
