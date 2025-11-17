/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.event.spi;

import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;

/**
 * Envers-specific collection recreation event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversPostCollectionRecreateEventListenerImpl
		extends BaseEnversCollectionEventListener
		implements PostCollectionRecreateEventListener {

	public EnversPostCollectionRecreateEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		final CollectionEntry collectionEntry = getCollectionEntry( event );
		if ( !collectionEntry.getLoadedPersister().isInverse() ) {
			onCollectionAction( event, event.getCollection(), null, collectionEntry );
		}
		else {
			onCollectionActionInversed( event, event.getCollection(), null, collectionEntry );
		}
	}
}
