//$Id$
package org.hibernate.ejb.event;

import java.io.Serializable;

import org.hibernate.event.EventSource;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Overrides the LifeCycle OnSave call to call the PreRemove operation
 *
 * @author Emmanuel Bernard
 */
public class EJB3DeleteEventListener extends DefaultDeleteEventListener implements CallbackHandlerConsumer {
	private EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3DeleteEventListener() {
		super();
	}

	public EJB3DeleteEventListener(EntityCallbackHandler callbackHandler) {
		this();
		this.callbackHandler = callbackHandler;
	}

	@Override
	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		callbackHandler.preRemove( entity );
		return super.invokeDeleteLifecycle( session, entity, persister );
	}

	@Override
	protected void performDetachedEntityDeletionCheck(DeleteEvent event) {
		EventSource source = event.getSession();
		String entityName = event.getEntityName();
		EntityPersister persister = source.getEntityPersister( entityName, event.getObject() );
		Serializable id =  persister.getIdentifier( event.getObject(), source.getEntityMode() );
		entityName = entityName == null ? source.guessEntityName( event.getObject() ) : entityName; 
		throw new IllegalArgumentException("Removing a detached instance "+ entityName + "#" + id);
	}
}
