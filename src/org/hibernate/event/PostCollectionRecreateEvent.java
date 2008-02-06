//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs after a collection is recreated
 *
 * @author Gail Badner
 */
public class PostCollectionRecreateEvent extends AbstractCollectionEvent {

	public PostCollectionRecreateEvent( CollectionPersister collectionPersister,
										PersistentCollection collection,
										EventSource source ) {
		super( collectionPersister, collection, source,
				collection.getOwner(),
				getOwnerIdOrNull( collection.getOwner(), source ) );
	}
}
