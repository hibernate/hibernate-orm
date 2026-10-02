package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Occurs after an entity instance is fully loaded.
 *
 * @author Kabir Khan
 */
public interface PostLoadEventListener {
	void onPostLoad(@Nonnull PostLoadEvent event);
}
