package org.hibernate.event.spi;

/**
 * Called after deleting an item from the datastore
 *
 * @author Gavin King
 */
public interface PostDeleteEventListener extends PostActionEventListener {
	void onPostDelete(PostDeleteEvent event);
}
