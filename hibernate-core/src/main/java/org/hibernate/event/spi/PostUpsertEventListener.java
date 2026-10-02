package org.hibernate.event.spi;

/**
 * Called after updating the datastore
 *
 * @author Gavin King
 */
public interface PostUpsertEventListener extends PostActionEventListener {
	void onPostUpsert(PostUpsertEvent event);
}
