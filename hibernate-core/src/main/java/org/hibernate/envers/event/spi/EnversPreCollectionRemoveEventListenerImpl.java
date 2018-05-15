/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.event.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;

/**
 * Envers-specific collection removal event listener
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class EnversPreCollectionRemoveEventListenerImpl
		extends BaseEnversCollectionEventListener
		implements PreCollectionRemoveEventListener {

	public EnversPreCollectionRemoveEventListenerImpl(AuditService auditService) {
		super( auditService );
	}

	@Override
	public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
		final CollectionEntry collectionEntry = getCollectionEntry( event );
		if ( collectionEntry != null ) {
			if ( !collectionEntry.getLoadedCollectionDescriptor().isInverse() ) {
				Serializable oldColl = collectionEntry.getSnapshot();
				if ( !event.getCollection().wasInitialized() && shouldGenerateRevision( event ) ) {
					// In case of uninitialized collection we need a fresh snapshot to properly calculate audit data.
					oldColl = initializeCollection( event );
				}
				onCollectionAction( event, null, oldColl, collectionEntry );
			}
			else {
				// HHH-7510 - Avoid LazyInitializationException when global_with_modified_flag = true
				if ( getAuditService().getOptions().isGlobalWithModifiedFlagEnabled() ) {
					initializeCollection( event );
				}
			}
		}
	}
}
