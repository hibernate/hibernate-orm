/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
 * @author Steve Ebersole
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
			throw new HibernateException( "Entity '" + lockable.getEntityName()
						+ "' may not be locked at level " + lockMode );
		}
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "Entity '" + lockable.getEntityName()
						+ "' has no version and may not be locked at level " + lockMode);
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, Timeout timeout, SharedSessionContractImplementor session) {
//		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( object );
		// Register the EntityIncrementVersionProcess action to run just prior to transaction commit.
		if ( session instanceof EventSource eventSource ) {
			eventSource.getActionQueue().registerCallback( new EntityIncrementVersionProcess( object ) );
		}
		else {
			throw new UnsupportedOperationException( "Optimistic locking strategies not supported in stateless session" );
		}
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
