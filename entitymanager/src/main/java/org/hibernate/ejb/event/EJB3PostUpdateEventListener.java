// $Id$
/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
