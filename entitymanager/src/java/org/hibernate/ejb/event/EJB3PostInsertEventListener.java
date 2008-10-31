/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.hibernate.ejb.event;

import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class EJB3PostInsertEventListener implements PostInsertEventListener, CallbackHandlerConsumer {
	EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PostInsertEventListener() {
		super();
	}

	public EJB3PostInsertEventListener(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void onPostInsert(PostInsertEvent event) {
		Object entity = event.getEntity();
		callbackHandler.postCreate( entity );
	}
}
