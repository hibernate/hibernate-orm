/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import jakarta.annotation.Nonnull;

/// Builds Jakarta Persistence callbacks for an entity
///
/// @author Steve Ebersole
public class EntityCallbacksFactory {
	@Nonnull
	public static EntityCallbacks<Object> buildCallbacks(
			@Nonnull PersistentClass persistentClass,
			@Nonnull SessionFactoryOptions options,
			@Nonnull ServiceRegistry serviceRegistry) {
		if ( !options.areJPACallbacksEnabled() || persistentClass.getClassName() == null ) {
			return NoCallbacks.NO_CALLBACKS;
		}

		final var beanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		final var callbacksCollector = new EntityCallbacksImpl.Builder<>();

		if ( isNotEmpty( persistentClass.getCallbackDefinitions() ) ) {
			for ( var definition : persistentClass.getCallbackDefinitions() ) {
				callbacksCollector.registerCallback( definition.createCallback( beanRegistry ) );
			}
		}

		return callbacksCollector.build();
	}
}
