package org.hibernate.event.spi;

/**
 * Called after recreating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRecreateEventListener {
	void onPostRecreateCollection(PostCollectionRecreateEvent event);
}
