//$Id: $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;

/**
 * An event that occurs before a collection is removed
 *
 * @author Gail Badner
 */
public class PreCollectionRemoveEvent extends AbstractCollectionEvent {

	public PreCollectionRemoveEvent(PersistentCollection collection, Object loadedOwner, EventSource source) {
		super( collection, source,
				loadedOwner,
				getOwnerIdOrNull( loadedOwner, source ) );
	}
}
