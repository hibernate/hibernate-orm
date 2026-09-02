/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.dialect.lock.spi.LockingStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

/**
 * An optimistic locking strategy that simply verifies that the
 * version has not changed, just before committing the transaction.
 * <p>
 * This strategy is valid for {@link LockMode#OPTIMISTIC}.
 *
 * @author Scott Marlow
 * @author Steve Ebersole
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
		// Register the EntityVerifyVersionProcess action to run just prior to transaction commit.
		if ( session instanceof EventSource eventSource ) {
			eventSource.getActionQueue().registerCallback( new EntityVerifyVersionProcess( object ) );
		}
		else {
			throw new UnsupportedOperationException( "Optimistic locking strategies not supported in stateless session" );
		}
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}
