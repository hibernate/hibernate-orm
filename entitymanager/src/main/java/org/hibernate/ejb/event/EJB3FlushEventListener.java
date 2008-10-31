//$Id$
package org.hibernate.ejb.event;

import org.hibernate.engine.CascadingAction;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.util.IdentityMap;

/**
 * In EJB3, it is the create operation that is cascaded to unmanaged
 * ebtities at flush time (instead of the save-update operation in
 * Hibernate).
 *
 * @author Gavin King
 */
public class EJB3FlushEventListener extends DefaultFlushEventListener {

	public static final FlushEventListener INSTANCE = new EJB3FlushEventListener();

	protected CascadingAction getCascadingAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}

	protected Object getAnything() {
		return IdentityMap.instantiate( 10 );
	}

}
