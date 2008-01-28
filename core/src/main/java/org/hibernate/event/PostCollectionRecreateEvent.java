//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs after a collection is recreated
 *
 * @author Gail Badner
 */
public class PostCollectionRecreateEvent extends AbstractCollectionEvent {

	public PostCollectionRecreateEvent(PersistentCollection collection, EventSource source) {
		super(collection, source, collection.getOwner());
	}
}
