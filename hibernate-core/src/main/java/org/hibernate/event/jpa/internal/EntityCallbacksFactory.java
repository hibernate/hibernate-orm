/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.boot.spi.CallbackDefinition;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

/// Builds Jakarta Persistence callbacks for an entity
///
/// @author Steve Ebersole
public class EntityCallbacksFactory {
	public static EntityCallbacks buildCallbacks(
			PersistentClass persistentClass,
			SessionFactoryOptions options,
			ServiceRegistry serviceRegistry) {
		if ( !options.areJPACallbacksEnabled() || persistentClass.getClassName() == null ) {
			return NoCallbacks.NO_CALLBACKS;
		}

		final var beanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		final var callbacksCollector = new EntityCallbacksImpl.Builder();

		if ( CollectionHelper.isNotEmpty( persistentClass.getCallbackDefinitions() ) ) {
			for ( CallbackDefinition definition : persistentClass.getCallbackDefinitions() ) {
				callbacksCollector.registerCallback( definition.createCallback( beanRegistry ) );
			}
		}

		return callbacksCollector.build();
	}
}
