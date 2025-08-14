/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostUpsertEvent;
import org.hibernate.event.spi.PostUpsertEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.persister.entity.EntityPersister;

/**
 * This is just a stub, since we don't yet have a {@code @PostUpsert} callback
 *
 * @author Gavin King
 */
public class PostUpsertEventListenerStandardImpl implements PostUpsertEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostUpsert(PostUpsertEvent event) {
		handlePostUpsert( event.getEntity(), event.getSession() );
	}

	private void handlePostUpsert(Object entity, SharedSessionContractImplementor source) {
//		// mimic the preUpdate filter
//		if ( source == null // it must be a StatelessSession
//				|| source.getPersistenceContextInternal().getEntry(entity).getStatus() != Status.DELETED ) {
//			callbackRegistry.postUpdate(entity);
//		}
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return false; //callbackRegistry.hasRegisteredCallbacks( persister.getMappedClass(), CallbackType.POST_UPDATE );
	}
}
