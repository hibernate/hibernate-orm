/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.SimpleSelect;

/**
 * A locking strategy where a lock is obtained via a select statement.
 * <p>
 * For non-read locks, this is achieved through the dialect's native
 * {@code SELECT ... FOR UPDATE} syntax.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(LockOptions, String)
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class SelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			int timeout,
			EventSource session) throws StaleObjectStateException, JDBCException {
		executeLock( id, version, object, timeout, session );
	}

	protected String generateLockString(int timeout) {
		final SessionFactoryImplementor factory = getLockable().getFactory();
		final LockOptions lockOptions = new LockOptions( getLockMode() );
		lockOptions.setTimeOut( timeout );
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
