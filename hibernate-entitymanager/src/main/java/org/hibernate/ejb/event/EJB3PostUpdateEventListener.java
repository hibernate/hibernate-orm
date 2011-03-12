/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.event;

import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;

/**
 * Implementation of the post update listeners.
 * 
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
@SuppressWarnings("serial")
public class EJB3PostUpdateEventListener implements PostUpdateEventListener,
		CallbackHandlerConsumer, PostCollectionRecreateEventListener,
		PostCollectionRemoveEventListener, PostCollectionUpdateEventListener {
	EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PostUpdateEventListener() {
		super();
	}

	public EJB3PostUpdateEventListener(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void onPostUpdate(PostUpdateEvent event) {
		Object entity = event.getEntity();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);
	}

	private void handlePostUpdate(Object entity, EventSource source) {
		EntityEntry entry = (EntityEntry) source.getPersistenceContext()
				.getEntityEntries().get(entity);
		// mimic the preUpdate filter
		if (Status.DELETED != entry.getStatus()) {
			callbackHandler.postUpdate(entity);
		}
	}

	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);
	}

	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);		
	}

	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		Object entity = event.getCollection().getOwner();
		EventSource eventSource = event.getSession();
		handlePostUpdate(entity, eventSource);		
	}
}
