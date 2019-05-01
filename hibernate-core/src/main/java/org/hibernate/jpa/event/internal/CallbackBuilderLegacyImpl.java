/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceException;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackBuilder;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import org.jboss.logging.Logger;

/**
 * EntityCallbackBuilder implementation using HCANN ReflectionManager.  "legacy" in that
 * we want to move to Jandex instead.
 *
 * @author Steve Ebersole
 */
final class CallbackBuilderLegacyImpl implements CallbackBuilder {
	private static final Logger log = Logger.getLogger( CallbackBuilderLegacyImpl.class );

	private final ManagedBeanRegistry managedBeanRegistry;
	private final ReflectionManager reflectionManager;

	CallbackBuilderLegacyImpl(ManagedBeanRegistry managedBeanRegistry, ReflectionManager reflectionManager) {
		this.managedBeanRegistry = managedBeanRegistry;
		this.reflectionManager = reflectionManager;
	}

	@Override
	public void buildCallbacksForEntity(String entityClassName, CallbackRegistrar callbackRegistrar) {
		try {
			final XClass entityXClass = reflectionManager.classForName( entityClassName );
			final Class entityClass = reflectionManager.toClass( entityXClass );
			for ( CallbackType callbackType : CallbackType.values() ) {
				if ( callbackRegistrar.hasRegisteredCallbacks( entityClass, callbackType ) ) {
					// this most likely means we have a class mapped multiple times using the hbm.xml
					// "entity name" feature
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"CallbackRegistry reported that Class [%s] already had %s callbacks registered; " +
										"assuming this means the class was mapped twice " +
										"(using hbm.xml entity-name support) - skipping subsequent registrations",
								entityClassName,
								callbackType.getCallbackAnnotation().getSimpleName()
						);
					}
					continue;
				}
				final Callback[] callbacks = resolveEntityCallbacks( entityXClass, callbackType, reflectionManager );
				callbackRegistrar.registerCallbacks( entityClass, callbacks );
			}
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "entity class not found: " + entityClassName, e );
		}
	}

	@Override
	public void buildCallbacksForEmbeddable(
			Property embeddableProperty, String entityClassName, CallbackRegistrar callbackRegistrar) {
		try {
			final XClass entityXClass = reflectionManager.classForName( entityClassName );
			final Class entityClass = reflectionManager.toClass( entityXClass );

			for ( CallbackType callbackType : CallbackType.values() ) {
				final Callback[] callbacks = resolveEmbeddableCallbacks(
						entityClass,
						embeddableProperty,
						callbackType,
						reflectionManager
				);
				callbackRegistrar.registerCallbacks( entityClass, callbacks );
			}
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "Class not found: ", e );
		}
	}

	@Override
	public void release() {
		// nothing to do
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public Callback[] resolveEntityCallbacks(XClass beanClass, CallbackType callbackType, ReflectionManager reflectionManager) {
		List<Callback> callbacks = new ArrayList<>();
		List<String> callbacksMethodNames = new ArrayList<>();
		List<Class> orderedListeners = new ArrayList<>();
		XClass currentClazz = beanClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;
		do {
			Callback callback = null;
			List<XMethod> methods = currentClazz.getDeclaredMethods();
			for ( final XMethod xMethod : methods ) {
				if ( xMethod.isAnnotationPresent( callbackType.getCallbackAnnotation() ) ) {
					Method method = reflectionManager.toMethod( xMethod );
					final String methodName = method.getName();
					if ( !callbacksMethodNames.contains( methodName ) ) {
						//overridden method, remove the superclass overridden method
						if ( callback == null ) {
							callback = new EntityCallback( method, callbackType );
							Class returnType = method.getReturnType();
							Class[] args = method.getParameterTypes();
							if ( returnType != Void.TYPE || args.length != 0 ) {
								throw new RuntimeException(
										"Callback methods annotated on the bean class must return void and take no arguments: "
												+ callbackType.getCallbackAnnotation().getName() + " - " + xMethod
								);
							}
							ReflectHelper.ensureAccessibility( method );
							if ( log.isDebugEnabled() ) {
								log.debugf(
										"Adding %s as %s callback for entity %s",
										methodName,
										callbackType.getCallbackAnnotation().getSimpleName(),
										beanClass.getName()
								);
							}
							callbacks.add( 0, callback ); //superclass first
							callbacksMethodNames.add( 0, methodName );
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + beanClass.getName()
							);
						}
					}
				}
			}
			if ( !stopListeners ) {
				getListeners( currentClazz, orderedListeners );
				stopListeners = currentClazz.isAnnotationPresent( ExcludeSuperclassListeners.class );
				stopDefaultListeners = currentClazz.isAnnotationPresent( ExcludeDefaultListeners.class );
			}

			do {
				currentClazz = currentClazz.getSuperclass();
			}
			while ( currentClazz != null
					&& !( currentClazz.isAnnotationPresent( Entity.class )
					|| currentClazz.isAnnotationPresent( MappedSuperclass.class ) )
					);
		}
		while ( currentClazz != null );

		//handle default listeners
		if ( !stopDefaultListeners ) {
			List<Class> defaultListeners = (List<Class>) reflectionManager.getDefaults().get( EntityListeners.class );

			if ( defaultListeners != null ) {
				int defaultListenerSize = defaultListeners.size();
				for ( int i = defaultListenerSize - 1; i >= 0; i-- ) {
					orderedListeners.add( defaultListeners.get( i ) );
				}
			}
		}

		for ( Class listener : orderedListeners ) {
			Callback callback = null;
			if ( listener != null ) {
				XClass xListener = reflectionManager.toXClass( listener );
				callbacksMethodNames = new ArrayList<>();
				List<XMethod> methods = xListener.getDeclaredMethods();
				for ( final XMethod xMethod : methods ) {
					if ( xMethod.isAnnotationPresent( callbackType.getCallbackAnnotation() ) ) {
						final Method method = reflectionManager.toMethod( xMethod );
						final String methodName = method.getName();
						if ( !callbacksMethodNames.contains( methodName ) ) {
							//overridden method, remove the superclass overridden method
							if ( callback == null ) {
								callback = new ListenerCallback(
										managedBeanRegistry.getBean( listener ),
										method,
										callbackType
								);

								Class returnType = method.getReturnType();
								Class[] args = method.getParameterTypes();
								if ( returnType != Void.TYPE || args.length != 1 ) {
									throw new PersistenceException(
											"Callback methods annotated in a listener bean class must return void and take one argument: "
													+ callbackType.getCallbackAnnotation().getName() + " - " + method
									);
								}
								ReflectHelper.ensureAccessibility( method );
								if ( log.isDebugEnabled() ) {
									log.debugf(
											"Adding %s as %s callback for entity %s",
											methodName,
											callbackType.getCallbackAnnotation().getSimpleName(),
											beanClass.getName()
										);
								}
								callbacks.add( 0, callback ); // listeners first
							}
							else {
								throw new PersistenceException(
										"You can only annotate one callback method with "
												+ callbackType.getCallbackAnnotation().getName()
												+ " in bean class: " + beanClass.getName()
												+ " and callback listener: " + listener.getName()
								);
							}
						}
					}
				}
			}
		}
		return callbacks.toArray( new Callback[callbacks.size()] );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public Callback[] resolveEmbeddableCallbacks(Class entityClass, Property embeddableProperty, CallbackType callbackType, ReflectionManager reflectionManager) {

		final String embeddableClassName = embeddableProperty.getType().getReturnedClass().getName();
		final XClass embeddableXClass = reflectionManager.classForName( embeddableClassName );
		final Getter embeddableGetter = embeddableProperty.getGetter( entityClass );
		final List<Callback> callbacks = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		XClass currentClazz = embeddableXClass;
		do {
			Callback callback = null;
			List<XMethod> methods = currentClazz.getDeclaredMethods();
			for ( final XMethod xMethod : methods ) {
				if ( xMethod.isAnnotationPresent( callbackType.getCallbackAnnotation() ) ) {
					Method method = reflectionManager.toMethod( xMethod );
					final String methodName = method.getName();
					if ( !callbacksMethodNames.contains( methodName ) ) {
						//overridden method, remove the superclass overridden method
						if ( callback == null ) {
							callback = new EmbeddableCallback( embeddableGetter, method, callbackType );
							Class returnType = method.getReturnType();
							Class[] args = method.getParameterTypes();
							if ( returnType != Void.TYPE || args.length != 0 ) {
								throw new RuntimeException(
										"Callback methods annotated on the bean class must return void and take no arguments: "
												+ callbackType.getCallbackAnnotation().getName() + " - " + xMethod
								);
							}
							ReflectHelper.ensureAccessibility( method );
							if ( log.isDebugEnabled() ) {
								log.debugf(
										"Adding %s as %s callback for entity %s",
										methodName,
										callbackType.getCallbackAnnotation().getSimpleName(),
										embeddableXClass.getName()
								);
							}
							callbacks.add( 0, callback ); //superclass first
							callbacksMethodNames.add( 0, methodName );
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + embeddableXClass.getName()
							);
						}
					}
				}
			}

			do {
				currentClazz = currentClazz.getSuperclass();
			}
			while ( currentClazz != null && !currentClazz.isAnnotationPresent( MappedSuperclass.class ) );
		}
		while ( currentClazz != null );

		return callbacks.toArray( new Callback[callbacks.size()] );
	}

	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		Target target = EntityListeners.class.getAnnotation( Target.class );
		if ( target != null ) {
			for ( ElementType type : target.value() ) {
				if ( type.equals( ElementType.ANNOTATION_TYPE ) ) {
					useAnnotationAnnotatedByListener = true;
				}
			}
		}
	}

	private static void getListeners(XClass currentClazz, List<Class> orderedListeners) {
		EntityListeners entityListeners = currentClazz.getAnnotation( EntityListeners.class );
		if ( entityListeners != null ) {
			Class[] classes = entityListeners.value();
			int size = classes.length;
			for ( int index = size - 1; index >= 0; index-- ) {
				orderedListeners.add( classes[index] );
			}
		}
		if ( useAnnotationAnnotatedByListener ) {
			Annotation[] annotations = currentClazz.getAnnotations();
			for ( Annotation annot : annotations ) {
				entityListeners = annot.getClass().getAnnotation( EntityListeners.class );
				if ( entityListeners != null ) {
					Class[] classes = entityListeners.value();
					int size = classes.length;
					for ( int index = size - 1; index >= 0; index-- ) {
						orderedListeners.add( classes[index] );
					}
				}
			}
		}
	}
}
