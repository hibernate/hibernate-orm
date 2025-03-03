/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * The intent of this class is to use a lighter implementation
 * when standard JPA entity lifecycle callbacks are disabled via
 * {@link SessionFactoryOptions#areJPACallbacksEnabled()}.
 */
public final class CallbacksFactory {
	private static final Logger log = Logger.getLogger( CallbacksFactory.class );

	public static CallbackRegistry buildCallbackRegistry(
			SessionFactoryOptions options, ServiceRegistry serviceRegistry, Collection<PersistentClass> entityBindings) {
		if ( !options.areJPACallbacksEnabled() ) {
			return new EmptyCallbackRegistryImpl();
		}
		final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		final CallbackRegistryImpl.Builder registryBuilder = new CallbackRegistryImpl.Builder();
		final Set<Class<?>> entityClasses = new HashSet<>();

		for ( PersistentClass persistentClass : entityBindings ) {
			if ( persistentClass.getClassName() != null ) {
				final Class<?> entityClass = persistentClass.getMappedClass();
				if ( !entityClasses.add( entityClass ) ) {
					// this most likely means we have a class mapped multiple
					// times using the hbm.xml "entity name" feature
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Class [%s] already has callbacks registered; " +
								"assuming this means the class was mapped twice " +
								"(using hbm.xml entity-name support) - skipping subsequent registrations" +
								"to avoid duplicates",
								entityClass.getName()
						);
					}
				}
				else {
					registerAllCallbacks( persistentClass, registryBuilder, entityClass, beanRegistry );
				}
			}
			// else we can have dynamic (non-java class) mapping
		}

		return registryBuilder.build();
	}

	private static void registerAllCallbacks(
			PersistentClass persistentClass,
			CallbackRegistryImpl.Builder registryBuilder,
			Class<?> entityClass,
			ManagedBeanRegistry beanRegistry) {
		registryBuilder.registerCallbacks( entityClass,
				buildCallbacks( persistentClass.getCallbackDefinitions(), beanRegistry ) );

		for ( Property property : persistentClass.getDeclaredProperties() ) {
			registryBuilder.registerCallbacks( entityClass,
					buildCallbacks( property.getCallbackDefinitions(), beanRegistry ) );
		}
	}

	private static Callback[] buildCallbacks(List<CallbackDefinition> callbackDefinitions,
			ManagedBeanRegistry beanRegistry) {
		if ( callbackDefinitions == null || callbackDefinitions.isEmpty() ) {
			return null;
		}
		else {
			final List<Callback> callbacks = new ArrayList<>();
			for ( CallbackDefinition definition : callbackDefinitions ) {
				callbacks.add( definition.createCallback( beanRegistry ) );
			}
			return callbacks.toArray( new Callback[0] );
		}
	}

}
