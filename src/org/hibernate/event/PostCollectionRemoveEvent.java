//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs after a collection is removed
 *
 * @author Gail Badner
 */
public class PostCollectionRemoveEvent extends AbstractCollectionEvent {

	public PostCollectionRemoveEvent(PersistentCollection collection, Object loadedOwner, EventSource source) {
		super(collection, source, loadedOwner);
	}
}
