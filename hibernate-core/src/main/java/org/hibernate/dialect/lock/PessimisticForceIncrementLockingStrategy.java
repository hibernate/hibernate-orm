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
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Lockable;

/**
 * A pessimistic locking strategy that increments the version immediately (obtaining an exclusive write lock).
 * <p/>
 * This strategy is valid for LockMode.PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticForceIncrementLockingStrategy implements LockingStrategy {
	private final Lockable lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public PessimisticForceIncrementLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		// ForceIncrement can be used for PESSIMISTIC_READ, PESSIMISTIC_WRITE or PESSIMISTIC_FORCE_INCREMENT
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		final EntityEntry entry = session.getPersistenceContext().getEntry( object );
		final EntityPersister persister = entry.getPersister();
		final Object nextVersion = persister.forceVersionIncrement( entry.getId(), entry.getVersion(), session );
		entry.forceLocked( object, nextVersion );
	}

	/**
	 * Retrieve the specific lock mode defined.
	 *
	 * @return The specific lock mode.
	 */
	protected LockMode getLockMode() {
		return lockMode;
	}
}
