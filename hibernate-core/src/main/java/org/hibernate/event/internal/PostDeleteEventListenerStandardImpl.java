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
		Object entity = event.getEntity();
		event.getPersister().getEntityCallbacks().postRemove( entity );
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return persister.getEntityCallbacks().hasRegisteredCallbacks( CallbackType.POST_REMOVE );
	}
}
