package org.hibernate.event.spi;

/**
 * Called after updating the datastore
 *
 * @author Gavin King
 */
public interface PostUpdateEventListener extends PostActionEventListener {
	void onPostUpdate(PostUpdateEvent event);
}
