package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called before updating the datastore
 *
 * @author Gavin King
 */
public interface PreUpdateEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreUpdate(@Nonnull PreUpdateEvent event);
}
