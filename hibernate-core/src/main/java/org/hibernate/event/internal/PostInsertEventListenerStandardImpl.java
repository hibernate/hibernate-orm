/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public class PostInsertEventListenerStandardImpl implements PostInsertEventListener {
	@Override
	public void onPostInsert(@Nonnull PostInsertEvent event) {
		final Object entity = event.getEntity();
		final var callbacks = event.getPersister().getEntityCallbacks();
		event.getSession()
				.runEntityLifecycleCallback( () -> {
					callbacks.postCreate( entity );
					callbacks.postInsert( entity );
				} );
	}

	@Override
	public boolean requiresPostCommitHandling(@Nonnull EntityPersister persister) {
		final var callbacks = persister.getEntityCallbacks();
		return callbacks.hasRegisteredCallbacks( CallbackType.POST_PERSIST )
			|| callbacks.hasRegisteredCallbacks( CallbackType.POST_INSERT );
	}
}
