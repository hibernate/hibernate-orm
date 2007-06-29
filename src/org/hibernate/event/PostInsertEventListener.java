//$Id$
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called after insterting an item in the datastore
 * 
 * @author Gavin King
 */
public interface PostInsertEventListener extends Serializable {
	public void onPostInsert(PostInsertEvent event);
}
