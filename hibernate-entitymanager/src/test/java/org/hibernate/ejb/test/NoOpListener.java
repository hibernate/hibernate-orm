//$Id$
package org.hibernate.ejb.test;

import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreInsertEvent;

/**
 * @author Emmanuel Bernard
 */
public class NoOpListener implements PreInsertEventListener {
	public boolean onPreInsert(PreInsertEvent event) {
		return false;
	}
}
