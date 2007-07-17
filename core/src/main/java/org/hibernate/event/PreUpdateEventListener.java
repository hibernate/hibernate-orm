//$Id: PreUpdateEventListener.java 11272 2007-03-12 00:17:45Z epbernard $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before updating the datastore
 * 
 * @author Gavin King
 */
public interface PreUpdateEventListener extends Serializable {
	/**
	 * Return true if the operation should be vetoed
	 */
	public boolean onPreUpdate(PreUpdateEvent event);
}
