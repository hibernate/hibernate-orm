package org.hibernate.event.spi;

/**
 * Called before updating the datastore
 *
 * @author Gavin King
 */
public interface PreUpdateEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreUpdate(PreUpdateEvent event);
}
