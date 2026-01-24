/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public class PostInsertEventListenerStandardImpl implements PostInsertEventListener {
	@Override
	public void onPostInsert(PostInsertEvent event) {
		Object entity = event.getEntity();
		event.getPersister().getEntityCallbacks().postCreate( entity );
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return persister.getEntityCallbacks().hasRegisteredCallbacks( CallbackType.POST_PERSIST );
	}
}
