/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A pessimistic locking strategy where a lock is obtained by incrementing
 * the version immediately, obtaining an exclusive write lock by side effect.
 * <p>
 * This strategy is valid for {@link LockMode#PESSIMISTIC_FORCE_INCREMENT}.
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticForceIncrementLockingStrategy implements LockingStrategy {
	private final EntityPersister lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public PessimisticForceIncrementLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		// ForceIncrement can be used for PESSIMISTIC_READ, PESSIMISTIC_WRITE or PESSIMISTIC_FORCE_INCREMENT
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "Entity '" + lockable.getEntityName()
						+ "' may not be locked at level " + lockMode );
		}
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "Entity '" + lockable.getEntityName()
						+ "' has no version and may not be locked at level " + lockMode);
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session) {
		final var entry = session.getPersistenceContextInternal().getEntry( object );
		OptimisticLockHelper.forceVersionIncrement( object, entry, session );
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
