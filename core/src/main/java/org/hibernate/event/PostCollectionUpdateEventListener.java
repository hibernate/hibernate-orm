//$Id: $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after updating a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionUpdateEventListener extends Serializable {
	public void onPostUpdateCollection(PostCollectionUpdateEvent event);
}
