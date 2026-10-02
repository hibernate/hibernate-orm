package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after inserting an item in the datastore
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PostInsertEventListener extends PostActionEventListener {
	void onPostInsert(@Nonnull PostInsertEvent event);
}
