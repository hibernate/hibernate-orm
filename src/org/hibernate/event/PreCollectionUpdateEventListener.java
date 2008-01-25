package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before updating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionUpdateEventListener extends Serializable {
	public void onPreUpdateCollection(PreCollectionUpdateEvent event);
}
