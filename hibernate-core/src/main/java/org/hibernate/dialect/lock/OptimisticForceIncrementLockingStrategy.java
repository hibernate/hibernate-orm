/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An optimistic locking strategy that verifies that the version
 * has not changed and then forces an increment of the version,
 * just before committing the transaction.
 * <p>
 * This strategy is valid for {@link LockMode#OPTIMISTIC_FORCE_INCREMENT}.
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class OptimisticForceIncrementLockingStrategy implements LockingStrategy {
	private final EntityPersister lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public OptimisticForceIncrementLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.OPTIMISTIC_FORCE_INCREMENT ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, EventSource session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
//		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( object );
		// Register the EntityIncrementVersionProcess action to run just prior to transaction commit.
		session.getActionQueue().registerProcess( new EntityIncrementVersionProcess( object ) );
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
