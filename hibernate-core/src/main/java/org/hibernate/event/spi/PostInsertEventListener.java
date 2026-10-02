package org.hibernate.event.spi;

/**
 * Called after inserting an item in the datastore
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PostInsertEventListener extends PostActionEventListener {
	void onPostInsert(PostInsertEvent event);
}
