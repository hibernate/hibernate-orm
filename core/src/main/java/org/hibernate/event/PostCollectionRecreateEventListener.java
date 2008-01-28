//$Id: $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after recreating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRecreateEventListener extends Serializable {
	public void onPostRecreateCollection(PostCollectionRecreateEvent event);
}
