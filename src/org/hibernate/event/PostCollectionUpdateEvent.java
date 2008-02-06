//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs after a collection is updated
 *
 * @author Gail Badner
 */
public class PostCollectionUpdateEvent extends AbstractCollectionEvent {

	public PostCollectionUpdateEvent(CollectionPersister collectionPersister,
									 PersistentCollection collection,
									 EventSource source) {
		super( collectionPersister, collection, source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source ) );
	}
}
