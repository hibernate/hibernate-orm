package org.hibernate.event.spi;

/**
 * Called after updating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionUpdateEventListener {
	void onPostUpdateCollection(PostCollectionUpdateEvent event);
}
