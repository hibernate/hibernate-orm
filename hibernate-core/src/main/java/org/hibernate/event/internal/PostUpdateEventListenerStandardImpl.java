/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class PostUpdateEventListenerStandardImpl implements PostUpdateEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		handlePostUpdate( event.getEntity(), event.getSession() );
	}

	private void handlePostUpdate(Object entity, EventSource source) {
		// mimic the preUpdate filter
		if ( source == null // it must be a StatelessSession
				|| source.getPersistenceContextInternal().getEntry(entity).getStatus() != Status.DELETED ) {
			callbackRegistry.postUpdate(entity);
		}
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return callbackRegistry.hasRegisteredCallbacks( persister.getMappedClass(), CallbackType.POST_UPDATE );
	}
}
