/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostUpsertEvent;
import org.hibernate.event.spi.PostUpsertEventListener;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class PostUpsertEventListenerStandardImpl implements PostUpsertEventListener {
	@Override
	public void onPostUpsert(PostUpsertEvent event) {
		handlePostUpsert( event.getEntity(), event.getPersister(), event.getSession() );
	}

	private void handlePostUpsert(
			Object entity,
			EntityPersister persister,
			SharedSessionContractImplementor source) {
		source.runEntityLifecycleCallback( () -> persister.getEntityCallbacks().postUpsert( entity ) );
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return persister.getEntityCallbacks().hasRegisteredCallbacks( CallbackType.POST_UPSERT );
	}
}
