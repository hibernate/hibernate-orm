//$Id: $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before recreating a collection
 *
 * @author Gail Badner
 */
public interface PreCollectionRecreateEventListener extends Serializable {
	public void onPreRecreateCollection(PreCollectionRecreateEvent event);
}
