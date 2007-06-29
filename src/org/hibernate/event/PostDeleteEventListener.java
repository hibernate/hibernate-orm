//$Id$
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after deleting an item from the datastore
 * 
 * @author Gavin King
 */
public interface PostDeleteEventListener extends Serializable {
	public void onPostDelete(PostDeleteEvent event);
}
