/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.Lockable;

/**
 * An optimistic locking strategy that simply verifies that the
 * version has not changed, just before committing the transaction.
 * <p>
 * This strategy is valid for {@link LockMode#OPTIMISTIC}.
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class OptimisticLockingStrategy implements LockingStrategy {
	private final Lockable lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public OptimisticLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.OPTIMISTIC ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, EventSource session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		// Register the EntityVerifyVersionProcess action to run just prior to transaction commit.
		session.getActionQueue().registerProcess( new EntityVerifyVersionProcess( object ) );
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
