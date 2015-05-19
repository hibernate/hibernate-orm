/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;

/**
 * Envers-specific collection removal event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EnversPreCollectionRemoveEventListenerImpl
		extends BaseEnversCollectionEventListener
		implements PreCollectionRemoveEventListener {

	public EnversPreCollectionRemoveEventListenerImpl(EnversService enversService) {
		super( enversService );
	}

	@Override
	public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
		final CollectionEntry collectionEntry = getCollectionEntry( event );
		if ( collectionEntry != null && !collectionEntry.getLoadedPersister().isInverse() ) {
			Serializable oldColl = collectionEntry.getSnapshot();
			if ( !event.getCollection().wasInitialized() && shouldGenerateRevision( event ) ) {
				// In case of uninitialized collection we need a fresh snapshot to properly calculate audit data.
				oldColl = initializeCollection( event );
			}
			onCollectionAction( event, null, oldColl, collectionEntry );
		}
	}
}
