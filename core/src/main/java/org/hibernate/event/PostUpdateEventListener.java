//$Id: PostUpdateEventListener.java 7581 2005-07-20 22:48:22Z oneovthafew $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after updating the datastore
 * 
 * @author Gavin King
 */
public interface PostUpdateEventListener extends Serializable {
	public void onPostUpdate(PostUpdateEvent event);
}
