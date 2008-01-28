//$Id: $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before removing a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRemoveEventListener extends Serializable {
	public void onPreRemoveCollection(PreCollectionRemoveEvent event);
}
