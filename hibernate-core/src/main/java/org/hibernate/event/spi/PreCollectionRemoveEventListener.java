package org.hibernate.event.spi;

/**
 * Called before removing a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRemoveEventListener {
	void onPreRemoveCollection(PreCollectionRemoveEvent event);
}
