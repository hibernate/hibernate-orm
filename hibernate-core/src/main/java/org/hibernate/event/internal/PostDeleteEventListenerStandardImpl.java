/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The standard PostDeleteEventListener implementation
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public class PostDeleteEventListenerStandardImpl implements PostDeleteEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	public void onPostDelete(PostDeleteEvent event) {
		Object entity = event.getEntity();
		callbackRegistry.postRemove( entity );
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return callbackRegistry.hasRegisteredCallbacks( persister.getMappedClass(), CallbackType.POST_REMOVE );
	}
}
