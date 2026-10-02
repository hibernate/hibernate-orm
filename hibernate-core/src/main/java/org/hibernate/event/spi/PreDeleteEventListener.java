package org.hibernate.event.spi;

/**
 * Called before deleting an item from the datastore
 *
 * @author Gavin King
 */
public interface PreDeleteEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreDelete(PreDeleteEvent event);
}
