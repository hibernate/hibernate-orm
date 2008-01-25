//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs after a collection is updated
 *
 * @author Gail Badner
 */
public class PostCollectionUpdateEvent extends AbstractCollectionEvent {

	public PostCollectionUpdateEvent(PersistentCollection collection, EventSource source) {
		super(collection, source, getLoadedOwner( collection, source ));
	}
}
