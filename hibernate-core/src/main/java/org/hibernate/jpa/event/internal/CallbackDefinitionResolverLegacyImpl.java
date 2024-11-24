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

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;

import org.jboss.logging.Logger;

/**
 * Resolves JPA callback definitions using a HCANN ReflectionManager.
 * <p>
 * "legacy" in that we want to move to Jandex instead.
 *
 * @author Steve Ebersole
 */
public final class CallbackDefinitionResolverLegacyImpl {
	private static final Logger log = Logger.getLogger( CallbackDefinitionResolverLegacyImpl.class );

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public static List<CallbackDefinition> resolveEntityCallbacks(ReflectionManager reflectionManager,
			XClass entityClass, CallbackType callbackType) {
		List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		List<String> callbacksMethodNames = new ArrayList<>();
		List<Class> orderedListeners = new ArrayList<>();
		XClass currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;
		do {
			CallbackDefinition callbackDefinition = null;
			List<XMethod> methods = currentClazz.getDeclaredMethods();
			for ( final XMethod xMethod : methods ) {
				if ( xMethod.isAnnotationPresent( callbackType.getCallbackAnnotation() ) ) {
					Method method = reflectionManager.toMethod( xMethod );
					final String methodName = method.getName();
					if ( !callbacksMethodNames.contains( methodName ) ) {
						//overridden method, remove the superclass overridden method
						if ( callbackDefinition == null ) {
							callbackDefinition = new EntityCallback.Definition( method, callbackType );
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
										entityClass.getName()
								);
							}
							callbackDefinitions.add( 0, callbackDefinition ); //superclass first
							callbacksMethodNames.add( 0, methodName );
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + entityClass.getName()
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
			CallbackDefinition callbackDefinition = null;
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
							if ( callbackDefinition == null ) {
								callbackDefinition = new ListenerCallback.Definition( listener, method, callbackType );

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
											entityClass.getName()
										);
								}
								callbackDefinitions.add( 0, callbackDefinition ); // listeners first
							}
							else {
								throw new PersistenceException(
										"You can only annotate one callback method with "
												+ callbackType.getCallbackAnnotation().getName()
												+ " in bean class: " + entityClass.getName()
												+ " and callback listener: " + listener.getName()
								);
							}
						}
					}
				}
			}
		}
		return callbackDefinitions;
	}

	public static List<CallbackDefinition> resolveEmbeddableCallbacks(ReflectionManager reflectionManager,
			Class<?> entityClass, Property embeddableProperty,
			CallbackType callbackType) {
		final Class embeddableClass = embeddableProperty.getType().getReturnedClass();
		final XClass embeddableXClass = reflectionManager.toXClass( embeddableClass );
		final Getter embeddableGetter = embeddableProperty.getGetter( entityClass );
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		XClass currentClazz = embeddableXClass;
		do {
			CallbackDefinition callbackDefinition = null;
			List<XMethod> methods = currentClazz.getDeclaredMethods();
			for ( final XMethod xMethod : methods ) {
				if ( xMethod.isAnnotationPresent( callbackType.getCallbackAnnotation() ) ) {
					Method method = reflectionManager.toMethod( xMethod );
					final String methodName = method.getName();
					if ( !callbacksMethodNames.contains( methodName ) ) {
						//overridden method, remove the superclass overridden method
						if ( callbackDefinition == null ) {
							callbackDefinition = new EmbeddableCallback.Definition( embeddableGetter, method, callbackType );
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
							callbackDefinitions.add( 0, callbackDefinition ); //superclass first
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

		return callbackDefinitions;
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
