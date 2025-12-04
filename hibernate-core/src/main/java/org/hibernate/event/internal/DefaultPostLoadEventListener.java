/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;


import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;

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

		final var session = event.getSession();
		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}

		final var lockMode = entry.getLockMode();
		if ( lockMode.requiresVersion() ) {
			final var persister = entry.getPersister();
			if ( persister.isVersioned() ) {
				switch ( lockMode ) {
					case PESSIMISTIC_FORCE_INCREMENT:
						OptimisticLockHelper.forceVersionIncrement( entity, entry, session );
						break;
					case OPTIMISTIC_FORCE_INCREMENT:
						session.getActionQueue()
								.registerCallback( new EntityIncrementVersionProcess( entity ) );
						break;
					case OPTIMISTIC:
						session.getActionQueue()
								.registerCallback( new EntityVerifyVersionProcess( entity ) );
						break;
				}
			}
			else {
				throw new HibernateException( "Entity '" + persister.getEntityName()
							+ "' has no version and may not be locked at level " + lockMode);
			}
		}
	}
}
