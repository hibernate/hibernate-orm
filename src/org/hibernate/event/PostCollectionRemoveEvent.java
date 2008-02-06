//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs after a collection is removed
 *
 * @author Gail Badner
 */
public class PostCollectionRemoveEvent extends AbstractCollectionEvent {

	public PostCollectionRemoveEvent(CollectionPersister collectionPersister,
									 PersistentCollection collection,
									 EventSource source,
									 Object loadedOwner ) {
		super( collectionPersister, collection, source,
				loadedOwner,
				getOwnerIdOrNull( loadedOwner, source ) );
	}
}
