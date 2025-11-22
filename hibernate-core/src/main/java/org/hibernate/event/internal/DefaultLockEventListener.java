/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.DetachedObjectException;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.LockEventListener;



import static org.hibernate.loader.ast.internal.LoaderHelper.upgradeLock;

/**
 * Defines the default lock event listeners used by hibernate to lock entities
 * in response to generated lock events.
 *
 * @author Steve Ebersole
 */
public class DefaultLockEventListener implements LockEventListener {

	/**
	 * Handle the given lock event.
	 *
	 * @param event The lock event to be handled.
	 */
	@Override
	public void onLock(LockEvent event) throws HibernateException {

		final Object instance = event.getObject();
		if ( instance == null ) {
			throw new NullPointerException( "Attempted to lock null" );
		}

		final var lockMode = event.getLockMode();
		if ( lockMode == LockMode.WRITE || lockMode == LockMode.UPGRADE_SKIPLOCKED ) {
			throw new IllegalArgumentException( "Invalid lock mode '" + lockMode + "' passed to 'lock()'" );
		}

		final var source = event.getSession();
		final var persistenceContext = source.getPersistenceContextInternal();
		final Object entity = persistenceContext.unproxyLoadingIfNecessary( instance );
		//TODO: if instance was an uninitialized proxy, this is inefficient,
		//      resulting in two SQL selects

		final var entry = persistenceContext.getEntry( entity );
		if ( entry == null && instance == entity ) {
			throw new DetachedObjectException( "Given entity is not associated with the persistence context" );
		}

		upgradeLock( entity, entry, event.getLockOptions(), event.getSession() );
	}
}
