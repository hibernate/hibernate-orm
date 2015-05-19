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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;

import org.jboss.logging.Logger;

/**
 * A locking strategy where the locks are obtained through update statements.
 * <p/>
 * This strategy is not valid for read style locks.
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class UpdateLockingStrategy implements LockingStrategy {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			UpdateLockingStrategy.class.getName()
	);

	private final Lockable lockable;
	private final LockMode lockMode;
	private final String sql;

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public UpdateLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.UPGRADE ) ) {
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
	public void lock(
			Serializable id,
			Object version,
			Object object,
			int timeout,
			SessionImplementor session) throws StaleObjectStateException, JDBCException {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "write locks via update not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}

		// todo : should we additionally check the current isolation mode explicitly?
		final SessionFactoryImplementor factory = session.getFactory();
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
				if ( affected < 0 ) {
					if (factory.getStatistics().isStatisticsEnabled()) {
						factory.getStatisticsImplementor().optimisticFailure( lockable.getEntityName() );
					}
					throw new StaleObjectStateException( lockable.getEntityName(), id );
				}

			}
			finally {
				session.getJdbcCoordinator().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch ( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not lock: " + MessageHelper.infoString( lockable, id, session.getFactory() ),
					sql
			);
		}
	}

	protected String generateLockString() {
		final SessionFactoryImplementor factory = lockable.getFactory();
		final Update update = new Update( factory.getDialect() );
		update.setTableName( lockable.getRootTableName() );
		update.addPrimaryKeyColumns( lockable.getRootTableIdentifierColumnNames() );
		update.setVersionColumnName( lockable.getVersionColumnName() );
		update.addColumn( lockable.getVersionColumnName() );
		if ( factory.getSettings().isCommentsEnabled() ) {
			update.setComment( lockMode + " lock " + lockable.getEntityName() );
		}
		return update.toStatementString();
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
