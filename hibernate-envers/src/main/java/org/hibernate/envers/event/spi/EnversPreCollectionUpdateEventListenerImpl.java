/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;

/**
 * Envers-specific collection update event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversPreCollectionUpdateEventListenerImpl
		extends BaseEnversCollectionEventListener
		implements PreCollectionUpdateEventListener {

	public EnversPreCollectionUpdateEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
		final CollectionEntry collectionEntry = getCollectionEntry( event );
		if ( !collectionEntry.getLoadedPersister().isInverse() ) {
			onCollectionAction( event, event.getCollection(), collectionEntry.getSnapshot(), collectionEntry );
		}
		else {
			onCollectionActionInversed( event, event.getCollection(), collectionEntry.getSnapshot(), collectionEntry );
		}
	}
}
