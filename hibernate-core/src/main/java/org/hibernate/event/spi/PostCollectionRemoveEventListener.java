package org.hibernate.event.spi;

/**
 * Called after removing a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRemoveEventListener {
	void onPostRemoveCollection(PostCollectionRemoveEvent event);
}
