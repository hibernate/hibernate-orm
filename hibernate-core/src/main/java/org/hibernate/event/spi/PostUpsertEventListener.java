package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after updating the datastore
 *
 * @author Gavin King
 */
public interface PostUpsertEventListener extends PostActionEventListener {
	void onPostUpsert(@Nonnull PostUpsertEvent event);
}
