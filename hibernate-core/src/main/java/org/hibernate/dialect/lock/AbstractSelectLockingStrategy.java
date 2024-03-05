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

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Base {@link LockingStrategy} implementation to support implementations
 * based on issuing {@code SQL} {@code SELECT} statements
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

	protected abstract String generateLockString(int lockTimeout);

	protected String determineSql(int timeout) {
		if ( timeout == LockOptions.WAIT_FOREVER) {
			return waitForeverSql;
		}
		else if ( timeout == LockOptions.NO_WAIT) {
			return getNoWaitSql();
		}
		else if ( timeout == LockOptions.SKIP_LOCKED) {
			return getSkipLockedSql();
		}
		else {
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

	protected void executeLock(Object id, Object version, Object object, int timeout, EventSource session) {
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
						final EntityEntry entry = session.getPersistenceContext().getEntry( object );
						if ( entry != null && entry.isExistsInDatabase() ) {
							final StatisticsImplementor statistics = factory.getStatistics();
							if ( statistics.isStatisticsEnabled() ) {
								statistics.optimisticFailure( lockable.getEntityName() );
							}
							throw new StaleObjectStateException( lockable.getEntityName(), id );
						}
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
		catch ( SQLException e ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
			);
		}
	}
}
