/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;


import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Performs needed {@link EntityEntry#getLockMode()}-related processing.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DefaultPostLoadEventListener implements PostLoadEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostLoad(PostLoadEvent event) {
		final Object entity = event.getEntity();

		callbackRegistry.postLoad( entity );

		final EventSource session = event.getSession();
		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( entity );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}

		final LockMode lockMode = entry.getLockMode();
		if ( lockMode.requiresVersion() ) {
			final EntityPersister persister = entry.getPersister();
			if ( persister.isVersioned() ) {
				switch ( lockMode ) {
					case PESSIMISTIC_FORCE_INCREMENT:
						OptimisticLockHelper.forceVersionIncrement( entity, entry, session );
						break;
					case OPTIMISTIC_FORCE_INCREMENT:
						session.getActionQueue().registerProcess( new EntityIncrementVersionProcess( entity ) );
						break;
					case OPTIMISTIC:
						session.getActionQueue().registerProcess( new EntityVerifyVersionProcess( entity ) );
						break;
				}
			}
			else {
				throw new HibernateException("[" + lockMode
						+ "] not supported for non-versioned entities [" + persister.getEntityName() + "]");
			}
		}
	}
}
