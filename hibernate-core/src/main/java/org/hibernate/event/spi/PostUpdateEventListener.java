package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after updating the datastore
 *
 * @author Gavin King
 */
public interface PostUpdateEventListener extends PostActionEventListener {
	void onPostUpdate(@Nonnull PostUpdateEvent event);
}
