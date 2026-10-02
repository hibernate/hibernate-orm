package org.hibernate.event.spi;

/**
 * Called before recreating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRecreateEventListener {
	void onPreRecreateCollection(PreCollectionRecreateEvent event);
}
