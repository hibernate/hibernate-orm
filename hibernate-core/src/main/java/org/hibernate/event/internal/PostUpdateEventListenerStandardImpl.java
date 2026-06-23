/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * @author Steve Ebersole
 */
public class PostUpdateEventListenerStandardImpl implements PostUpdateEventListener {
	@Override
	public void onPostUpdate(@Nonnull PostUpdateEvent event) {
		handlePostUpdate( event.getEntity(), event.getPersister(), event.getSession() );
	}

	private void handlePostUpdate(
			@Nonnull Object entity,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		// mimic the preUpdate filter
		if ( source.isStateless()
				|| source.getPersistenceContextInternal().getEntry( entity ).getStatus() != Status.DELETED ) {
			source.runEntityLifecycleCallback( () -> persister.getEntityCallbacks().postUpdate( entity ) );
		}
	}

	@Override
	public boolean requiresPostCommitHandling(@Nonnull EntityPersister persister) {
		return persister.getEntityCallbacks().hasRegisteredCallbacks( CallbackType.POST_UPDATE );
	}
}
