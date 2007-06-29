//$Id$
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
