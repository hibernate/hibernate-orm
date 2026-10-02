package org.hibernate.event.spi;

/**
 * Called before updating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionUpdateEventListener {
	void onPreUpdateCollection(PreCollectionUpdateEvent event);
}
