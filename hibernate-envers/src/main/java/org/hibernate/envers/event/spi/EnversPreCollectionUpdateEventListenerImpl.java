/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	}
}
