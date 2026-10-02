package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after removing a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRemoveEventListener {
	void onPostRemoveCollection(@Nonnull PostCollectionRemoveEvent event);
}
