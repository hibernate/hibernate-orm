package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called before removing a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRemoveEventListener {
	void onPreRemoveCollection(@Nonnull PreCollectionRemoveEvent event);
}
