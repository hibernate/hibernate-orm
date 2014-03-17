/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.event.internal.jpa;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.spi.jpa.Callback;
import org.hibernate.jpa.event.spi.jpa.ListenerFactory;
import org.hibernate.metamodel.source.spi.JpaCallbackSource;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CallbackProcessorImpl implements CallbackProcessor {
	private static final Logger log = Logger.getLogger( CallbackProcessorImpl.class );

	private final ListenerFactory jpaListenerFactory;
	private final MetadataImplementor metadata;

	private final ClassLoaderService classLoaderService;

	public CallbackProcessorImpl(
			ListenerFactory jpaListenerFactory,
			MetadataImplementor metadata,
			SessionFactoryServiceRegistry serviceRegistry) {
		this.jpaListenerFactory = jpaListenerFactory;
		this.metadata = metadata;
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@Override
	public void processCallbacksForEntity(Object entityObject, CallbackRegistryImpl callbackRegistry) {
		final EntityBinding entityBinding = (EntityBinding) entityObject;
		if ( entityBinding.getHierarchyDetails().getEntityMode() != EntityMode.POJO ) {
			return;
		}
		final Class entityClass = classLoaderService.classForName(
				entityBinding.getEntity().getDescriptor().getName().toString()
		);
		for ( final Class annotationClass : CALLBACK_ANNOTATION_CLASSES ) {
			callbackRegistry.addEntityCallbacks(
					entityClass,
					annotationClass,
					collectCallbacks( entityBinding, entityClass, annotationClass )
			);
		}
	}

	private final static Callback[] EMPTY_CALLBACK = new Callback[0];

	private Callback[] collectCallbacks(EntityBinding entityBinding, Class entityClass, Class annotationClass) {
		final List<JpaCallbackSource> jpaCallbackSources = entityBinding.getJpaCallbackClasses();
		if ( CollectionHelper.isEmpty( jpaCallbackSources ) ) {
			return EMPTY_CALLBACK;
		}
		final List<Callback> result = new ArrayList<Callback>( jpaCallbackSources.size() );
		for ( final JpaCallbackSource jpaCallbackSource : entityBinding.getJpaCallbackClasses() ) {
			final Class listenerClass = classLoaderService.classForName( jpaCallbackSource.getName() );
			final String methodName = jpaCallbackSource.getCallbackMethod( annotationClass );
			if ( methodName == null ) {
				continue;
			}
			log.debugf(
					"Adding %s.%s as %s callback for entity %s",
					listenerClass.getName(),
					methodName,
					annotationClass.getName(),
					entityClass.getName()
			);

			final Callback callback = jpaCallbackSource.isListener()
					? createListenerCallback( listenerClass, entityClass, methodName )
					: createBeanCallback( listenerClass, methodName );
			if ( callback != null ) {
				result.add( callback );
			}
		}
		return result.toArray( new Callback[result.size()] );
	}

	private Callback createListenerCallback(
			Class listenerClass,
			Class entityClass,
			String methodName ) {
		final Class<?> callbackSuperclass = listenerClass.getSuperclass();
		if ( callbackSuperclass != null ) {
			Callback callback = createListenerCallback( entityClass, callbackSuperclass, methodName );
			if ( callback != null ) {
				return callback;
			}
		}

		final Object listenerInstance = jpaListenerFactory.buildListener( listenerClass );
		for ( Method method : listenerClass.getDeclaredMethods() ) {
			if ( !method.getName().equals(methodName) ) {
				continue;
			}

			final Class<?>[] argTypes = method.getParameterTypes();
			if (argTypes.length != 1) {
				continue;
			}

			final Class<?> argType = argTypes[0];
			if (argType != Object.class && argType != entityClass) {
				continue;
			}
			method.setAccessible( true );

			return new ListenerCallback( listenerInstance, method );
		}
		return null;
	}

	private Callback createBeanCallback( Class<?> callbackClass,
												String methodName ) {
		Class<?> callbackSuperclass = callbackClass.getSuperclass();
		if (callbackSuperclass != null) {
			Callback callback = createBeanCallback(callbackSuperclass, methodName);
			if (callback != null) return callback;
		}
		for (Method method : callbackClass.getDeclaredMethods()) {
			if (!method.getName().equals(methodName)) continue;
			if (method.getParameterTypes().length != 0) continue;
			method.setAccessible(true);
			return new EntityCallback(method);
		}
		return null;
	}

	@Override
	public void release() {
	}
}
