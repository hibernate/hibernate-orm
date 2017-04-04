/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;

/**
 * An event that occurs when a collection wants to be
 * initialized
 * 
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractCollectionEvent {
	public InitializeCollectionEvent(PersistentCollection collection, EventSource source ) {
		super( getLoadedCollectionPersister( collection, source ),
				collection,
				source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source ) );
	}
}
