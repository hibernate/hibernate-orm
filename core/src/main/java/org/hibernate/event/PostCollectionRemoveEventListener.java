//$Id: $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after removing a collection
 *
 * @author Gail Badner
 */
public interface PostCollectionRemoveEventListener extends Serializable {
	public void onPostRemoveCollection(PostCollectionRemoveEvent event);
}
