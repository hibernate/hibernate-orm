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

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implementation of the post update listeners.
 * 
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
@SuppressWarnings("serial")
public class JpaPostUpdateEventListener
		implements PostUpdateEventListener,
				   CallbackRegistryConsumer,
				   PostCollectionRecreateEventListener,
				   PostCollectionRemoveEventListener,
				   PostCollectionUpdateEventListener {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	public JpaPostUpdateEventListener() {
		super();
	}

	public JpaPostUpdateEventListener(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		Object entity = event.getEntity();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);
	}

	private void handlePostUpdate(Object entity, EventSource source) {
		EntityEntry entry = (EntityEntry) source.getPersistenceContext().getEntry( entity );
		// mimic the preUpdate filter
		if ( Status.DELETED != entry.getStatus()) {
			callbackRegistry.postUpdate(entity);
		}
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return callbackRegistry.hasPostUpdateCallbacks( persister.getMappedClass() );
	}

	@Override
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);
	}

	@Override
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);		
	}

	@Override
	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);		
	}
}
