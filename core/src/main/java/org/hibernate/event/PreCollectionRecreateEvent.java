//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs before a collection is recreated
 *
 * @author Gail Badner
 */
public class PreCollectionRecreateEvent extends AbstractCollectionEvent {

	public PreCollectionRecreateEvent(CollectionPersister collectionPersister,
									  PersistentCollection collection,
									  EventSource source) {
		super( collectionPersister, collection, source,
				collection.getOwner(),
				getOwnerIdOrNull( collection.getOwner(), source ) );
	}
}
