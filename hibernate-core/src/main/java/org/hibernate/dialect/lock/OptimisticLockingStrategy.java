/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

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
	private final EntityPersister lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public OptimisticLockingStrategy(EntityPersister lockable, LockMode lockMode) {
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
