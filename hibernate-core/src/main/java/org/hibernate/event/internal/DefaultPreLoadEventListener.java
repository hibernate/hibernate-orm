/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly
 * loaded entity instance.
 *
 * @author Gavin King
 */
public class DefaultPreLoadEventListener implements PreLoadEventListener {

	@Override
	public void onPreLoad(PreLoadEvent event) {
		final EntityPersister persister = event.getPersister();
		event.getSession().getInterceptor().onLoad(
				event.getEntity(),
				event.getId(),
				event.getState(),
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);
	}

}
