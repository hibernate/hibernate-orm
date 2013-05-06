/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.event.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
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

	protected EnversPreCollectionRemoveEventListenerImpl(AuditConfiguration enversConfiguration) {
		super( enversConfiguration );
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
