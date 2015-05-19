/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

import java.io.Serializable;

import org.hibernate.event.internal.DefaultDeleteEventListener;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Overrides the LifeCycle OnSave call to call the PreRemove operation
 *
 * @author Emmanuel Bernard
 */
public class JpaDeleteEventListener extends DefaultDeleteEventListener implements CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	public JpaDeleteEventListener() {
		super();
	}

	public JpaDeleteEventListener(CallbackRegistry callbackRegistry) {
		this();
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		callbackRegistry.preRemove( entity );
		return super.invokeDeleteLifecycle( session, entity, persister );
	}

	@Override
	protected void performDetachedEntityDeletionCheck(DeleteEvent event) {
		EventSource source = event.getSession();
		String entityName = event.getEntityName();
		EntityPersister persister = source.getEntityPersister( entityName, event.getObject() );
		Serializable id =  persister.getIdentifier( event.getObject(), source );
		entityName = entityName == null ? source.guessEntityName( event.getObject() ) : entityName; 
		throw new IllegalArgumentException("Removing a detached instance "+ entityName + "#" + id);
	}
}
