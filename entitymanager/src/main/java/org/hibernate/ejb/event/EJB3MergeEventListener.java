//$Id$
package org.hibernate.ejb.event;

import java.io.Serializable;

import org.hibernate.event.EventSource;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Overrides the LifeCycle OnSave call to call the PrePersist operation
 *
 * @author Emmanuel Bernard
 */
public class EJB3MergeEventListener extends DefaultMergeEventListener implements CallbackHandlerConsumer {
	private EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3MergeEventListener() {
		super();
	}

	public EJB3MergeEventListener(EntityCallbackHandler callbackHandler) {
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
}
