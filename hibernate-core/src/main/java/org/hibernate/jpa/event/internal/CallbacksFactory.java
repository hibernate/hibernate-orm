/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 * when JPA callbacks are disabled via
 * {@link SessionFactoryOptions#areJPACallbacksEnabled()}
 */
public final class CallbacksFactory {
	private static final Logger log = Logger.getLogger( CallbacksFactory.class );

	public static CallbackRegistry buildCallbackRegistry(SessionFactoryOptions options, ServiceRegistry serviceRegistry, Collection<PersistentClass> entityBindings) {
		if ( !jpaCallBacksEnabled( options ) ) {
			return new EmptyCallbackRegistryImpl();
		}
		ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		CallbackRegistryImpl.Builder registryBuilder = new CallbackRegistryImpl.Builder();
		Set<Class<?>> entityClasses = new HashSet<>();

		for ( PersistentClass persistentClass : entityBindings ) {
			if ( persistentClass.getClassName() == null ) {
				// we can have dynamic (non-java class) mapping
				continue;
			}

			Class<?> entityClass = persistentClass.getMappedClass();

			if ( !entityClasses.add( entityClass ) ) {
				// this most likely means we have a class mapped multiple times using the hbm.xml
				// "entity name" feature
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Class [%s] already has callbacks registered; " +
									"assuming this means the class was mapped twice " +
									"(using hbm.xml entity-name support) - skipping subsequent registrations" +
									"to avoid duplicates",
							entityClass.getName()
					);
				}
				continue;
			}

			registryBuilder.registerCallbacks( persistentClass.getMappedClass(),
					buildCallbacks( persistentClass.getCallbackDefinitions(), beanRegistry ) );

			for ( Property property : persistentClass.getDeclaredProperties() ) {
				registryBuilder.registerCallbacks( persistentClass.getMappedClass(),
						buildCallbacks( property.getCallbackDefinitions(), beanRegistry ) );
			}
		}

		return registryBuilder.build();
	}

	private static Callback[] buildCallbacks(List<CallbackDefinition> callbackDefinitions,
			ManagedBeanRegistry beanRegistry) {
		if ( callbackDefinitions == null || callbackDefinitions.isEmpty() ) {
			return null;
		}
		List<Callback> callbacks = new ArrayList<>();
		for ( CallbackDefinition definition : callbackDefinitions ) {
			callbacks.add( definition.createCallback( beanRegistry ) );
		}
		return callbacks.toArray( new Callback[0] );
	}

	private static boolean jpaCallBacksEnabled(SessionFactoryOptions options) {
		return options.areJPACallbacksEnabled();
	}

}
