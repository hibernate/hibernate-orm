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

import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class EJB3PostDeleteEventListener implements PostDeleteEventListener, CallbackHandlerConsumer {
	EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PostDeleteEventListener() {
		super();
	}

	public EJB3PostDeleteEventListener(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void onPostDelete(PostDeleteEvent event) {
		Object entity = event.getEntity();
		callbackHandler.postRemove( entity );
	}

}
