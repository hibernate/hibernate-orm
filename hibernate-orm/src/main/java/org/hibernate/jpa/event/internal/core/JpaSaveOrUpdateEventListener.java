/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

import java.io.Serializable;

import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;

/**
 * Overrides the LifeCycle OnSave call to call the PrePersist operation
 *
 * @author Emmanuel Bernard
 */
public class JpaSaveOrUpdateEventListener extends DefaultSaveOrUpdateEventListener implements CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	public JpaSaveOrUpdateEventListener() {
		super();
	}

	public JpaSaveOrUpdateEventListener(CallbackRegistry callbackRegistry) {
		super();
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	protected Serializable saveWithRequestedId(
			Object entity,
			Serializable requestedId,
			String entityName,
			Object anything,
			EventSource source) {
		callbackRegistry.preCreate( entity );
		return super.saveWithRequestedId( entity, requestedId, entityName, anything, source );
	}

	@Override
	protected Serializable saveWithGeneratedId(
			Object entity,
			String entityName,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {
		callbackRegistry.preCreate( entity );
		return super.saveWithGeneratedId( entity, entityName, anything, source, requiresImmediateIdAccess );
	}
}
