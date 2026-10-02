package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * Called before inserting an item in the datastore
 *
 * @author Gavin King
 */
public interface PreInsertEventListener {
	/**
	 * Return true if the operation should be vetoed
	 */
	boolean onPreInsert(@Nonnull PreInsertEvent event);
}
