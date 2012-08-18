/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
