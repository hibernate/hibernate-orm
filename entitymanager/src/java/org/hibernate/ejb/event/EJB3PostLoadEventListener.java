/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.hibernate.ejb.event;

import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision$
 */
public class EJB3PostLoadEventListener implements PostLoadEventListener, CallbackHandlerConsumer {
	EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PostLoadEventListener() {
		super();
	}

	public EJB3PostLoadEventListener(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void onPostLoad(PostLoadEvent event) {
		Object entity = event.getEntity();
		callbackHandler.postLoad( entity );
	}

}
