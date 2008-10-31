//$Id$
package org.hibernate.ejb.event;

import java.io.Serializable;

import org.hibernate.event.EventSource;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EJB3CascadingAction;
import org.hibernate.engine.EJB3CascadeStyle;

/**
 * Overrides the LifeCycle OnSave call to call the PrePersist operation
 *
 * @author Emmanuel Bernard
 */
public class EJB3PersistEventListener extends DefaultPersistEventListener implements CallbackHandlerConsumer {
	static {
		EJB3CascadeStyle.PERSIST_EJB3.hasOrphanDelete(); //triggers class loading to override persist with PERSIST_EJB3
	}

	private EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PersistEventListener() {
		super();
	}

	public EJB3PersistEventListener(EntityCallbackHandler callbackHandler) {
		super();
		this.callbackHandler = callbackHandler;
	}

	@Override
	protected Serializable saveWithRequestedId(Object entity, Serializable requestedId, String entityName,
											   Object anything, EventSource source) {
		callbackHandler.preCreate( entity );
		return super.saveWithRequestedId( entity, requestedId, entityName, anything,
				source );
	}

	@Override
	protected Serializable saveWithGeneratedId(Object entity, String entityName, Object anything, EventSource source,
											   boolean requiresImmediateIdAccess) {
		callbackHandler.preCreate( entity );
		return super.saveWithGeneratedId( entity, entityName, anything, source,
				requiresImmediateIdAccess );
	}

	@Override
	protected CascadingAction getCascadeAction() {
		return EJB3CascadingAction.PERSIST_SKIPLAZY;
	}
}
