//$Id: PostDeleteEventListener.java 7581 2005-07-20 22:48:22Z oneovthafew $
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
