/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;

/**
 * An optimistic locking strategy that forces an increment of the version (after verifying that version hasn't changed).
 * This takes place just prior to transaction commit.
 * <p/>
 * This strategy is valid for LockMode.OPTIMISTIC_FORCE_INCREMENT
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class OptimisticForceIncrementLockingStrategy implements LockingStrategy {
	private final Lockable lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public OptimisticForceIncrementLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.OPTIMISTIC_FORCE_INCREMENT ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		final EntityEntry entry = session.getPersistenceContext().getEntry( object );
		// Register the EntityIncrementVersionProcess action to run just prior to transaction commit.
		( (EventSource) session ).getActionQueue().registerProcess( new EntityIncrementVersionProcess( object, entry ) );
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
