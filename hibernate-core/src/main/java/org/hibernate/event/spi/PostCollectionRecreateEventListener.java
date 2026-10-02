package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called after recreating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRecreateEventListener {
	void onPostRecreateCollection(@Nonnull PostCollectionRecreateEvent event);
}
