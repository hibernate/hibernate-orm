package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after deleting an item from the datastore
 *
 * @author Gavin King
 */
public interface PostDeleteEventListener extends PostActionEventListener {
	void onPostDelete(@Nonnull PostDeleteEvent event);
}
