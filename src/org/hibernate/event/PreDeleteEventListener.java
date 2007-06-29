//$Id$
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before deleting an item from the datastore
 * 
 * @author Gavin King
 */
public interface PreDeleteEventListener extends Serializable {
	/**
	 * Return true if the operation should be vetoed
	 */
	public boolean onPreDelete(PreDeleteEvent event);
}
