/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The standard PostDeleteEventListener implementation
 *
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public class PostDeleteEventListenerStandardImpl implements PostDeleteEventListener {
	@Override
	public void onPostDelete(PostDeleteEvent event) {
		final Object entity = event.getEntity();
		final var callbacks = event.getPersister().getEntityCallbacks();
		event.getSession()
				.runEntityLifecycleCallback( () -> {
					callbacks.postRemove( entity );
					callbacks.postDelete( entity );
				} );
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		final var callbacks = persister.getEntityCallbacks();
		return callbacks.hasRegisteredCallbacks( CallbackType.POST_REMOVE )
			|| callbacks.hasRegisteredCallbacks( CallbackType.POST_DELETE );
	}
}
