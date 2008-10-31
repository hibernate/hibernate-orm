//$Id$
package org.hibernate.ejb.event;

import org.hibernate.engine.CascadingAction;

/**
 * @author Emmanuel Bernard
 */
public class EJB3PersistOnFlushEventListener extends EJB3PersistEventListener {
	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}
}
