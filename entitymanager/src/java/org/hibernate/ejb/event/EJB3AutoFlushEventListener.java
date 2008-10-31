//$Id$
package org.hibernate.ejb.event;

import org.hibernate.engine.CascadingAction;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.util.IdentityMap;

/**
 * In EJB3, it is the create operation that is cascaded to unmanaged
 * ebtities at flush time (instead of the save-update operation in
 * Hibernate).
 *
 * @author Gavin King
 */
public class EJB3AutoFlushEventListener extends DefaultAutoFlushEventListener {

	public static final AutoFlushEventListener INSTANCE = new EJB3AutoFlushEventListener();

	protected CascadingAction getCascadingAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}

	protected Object getAnything() {
		return IdentityMap.instantiate( 10 );
	}

}
