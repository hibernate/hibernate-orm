/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.stat.spi.StatisticsImplementor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
	private final Lockable lockable;
	private final LockMode lockMode;
	private final String waitForeverSql;

	protected AbstractSelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.waitForeverSql = generateLockString( LockOptions.WAIT_FOREVER );
	}

	protected Lockable getLockable() {
		return lockable;
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected String generateLockString(int lockTimeout) {
		final SessionFactoryImplementor factory = lockable.getFactory();
		final LockOptions lockOptions = new LockOptions( lockMode );
		lockOptions.setTimeOut( lockTimeout );
		final SimpleSelect select =
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
	public void lock(Object id, Object version, Object object, int timeout, EventSource session)
			throws StaleObjectStateException, JDBCException {
		final String sql = determineSql( timeout );
		final SessionFactoryImplementor factory = session.getFactory();
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

				final ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st, sql );
				try {
					if ( !rs.next() ) {
						final StatisticsImplementor statistics = factory.getStatistics();
						if ( statistics.isStatisticsEnabled() ) {
							statistics.optimisticFailure( lockable.getEntityName() );
						}
						throw new StaleObjectStateException( lockable.getEntityName(), id );
					}
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException sqle ) {
			throw convertException( object, jdbcException( id, session, sqle, sql ) );
		}
	}

	private JDBCException jdbcException(Object id, EventSource session, SQLException sqle, String sql) {
		return session.getJdbcServices().getSqlExceptionHelper()
				.convert( sqle, "could not lock: " + infoString( lockable, id, session.getFactory() ), sql );
	}

	protected HibernateException convertException(Object entity, JDBCException ex) {
		return ex;
	}

	protected String determineSql(int timeout) {
		switch (timeout) {
			case LockOptions.WAIT_FOREVER:
				return waitForeverSql;
			case LockOptions.NO_WAIT:
				return getNoWaitSql();
			case LockOptions.SKIP_LOCKED:
				return getSkipLockedSql();
			default:
				return generateLockString( timeout );
		}
	}

	private String noWaitSql;

	protected String getNoWaitSql() {
		if ( noWaitSql == null ) {
			noWaitSql = generateLockString( LockOptions.NO_WAIT );
		}
		return noWaitSql;
	}

	private String skipLockedSql;

	protected String getSkipLockedSql() {
		if ( skipLockedSql == null ) {
			skipLockedSql = generateLockString( LockOptions.SKIP_LOCKED );
		}
		return skipLockedSql;
	}
}
