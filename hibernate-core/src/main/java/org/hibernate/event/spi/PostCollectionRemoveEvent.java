/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;

/**
 * An event that occurs afterQuery a collection is removed
 *
 * @author Gail Badner
 */
public class PostCollectionRemoveEvent extends AbstractCollectionEvent {
	public PostCollectionRemoveEvent(
			PersistentCollectionMetadata collectionPersister,
			PersistentCollection collection,
			EventSource source,
			Object loadedOwner) {
		super( collectionPersister, collection, source, loadedOwner, getOwnerIdOrNull( loadedOwner, source ) );
	}
}
