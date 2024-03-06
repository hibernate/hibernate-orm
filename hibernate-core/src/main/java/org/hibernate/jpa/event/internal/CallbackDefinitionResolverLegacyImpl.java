/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.property.access.spi.Getter;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;

/**
 * Resolves JPA callback definitions using a HCANN ReflectionManager.
 * <p>
 * "legacy" in that we want to move to Jandex instead.
 *
 * @author Steve Ebersole
 */
public final class CallbackDefinitionResolverLegacyImpl {
	private static final Logger log = Logger.getLogger( CallbackDefinitionResolverLegacyImpl.class );

	public static List<CallbackDefinition> resolveEntityCallbacks(
			ReflectionManager reflectionManager,
			ClassDetails entityClass,
			CallbackType callbackType) {
		List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		List<String> callbacksMethodNames = new ArrayList<>();
		List<Class<?>> orderedListeners = new ArrayList<>();
		ClassDetails currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;
		do {
			CallbackDefinition callbackDefinition = null;
			final List<MethodDetails> methodsDetailsList = currentClazz.getMethods();
			for ( MethodDetails methodDetails : methodsDetailsList ) {
				if ( !methodDetails.hasAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
					continue;
				}
				if ( callbacksMethodNames.contains( methodDetails.getName() ) ) {
					continue;
				}

				//overridden method, remove the superclass overridden method
				if ( callbackDefinition == null ) {
					final Method javaMethod = (Method) methodDetails.toJavaMember();
					callbackDefinition = new EntityCallback.Definition( javaMethod, callbackType );
					Class<?> returnType = javaMethod.getReturnType();
					Class<?>[] args = javaMethod.getParameterTypes();
					if ( returnType != Void.TYPE || args.length != 0 ) {
						throw new RuntimeException(
								"Callback methods annotated on the bean class must return void and take no arguments: "
										+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
						);
					}
					ReflectHelper.ensureAccessibility( javaMethod );
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Adding %s as %s callback for entity %s",
								methodDetails.getName(),
								callbackType.getCallbackAnnotation().getSimpleName(),
								entityClass.getName()
						);
					}
					callbackDefinitions.add( 0, callbackDefinition ); //superclass first
					callbacksMethodNames.add( 0, methodDetails.getName() );
				}
				else {
					throw new PersistenceException(
							"You can only annotate one callback method with "
									+ callbackType.getCallbackAnnotation().getName() + " in bean class: " + entityClass.getName()
					);
				}
			}
			if ( !stopListeners ) {
				applyListeners( currentClazz, orderedListeners );
				stopListeners = currentClazz.hasAnnotationUsage( ExcludeSuperclassListeners.class );
				stopDefaultListeners = currentClazz.hasAnnotationUsage( ExcludeDefaultListeners.class );
			}

			do {
				currentClazz = currentClazz.getSuperClass();
			}
			while ( currentClazz != null
					&& !( currentClazz.hasAnnotationUsage( Entity.class )
					|| currentClazz.hasAnnotationUsage( MappedSuperclass.class ) )
					);
		}
		while ( currentClazz != null );

		//handle default listeners
		if ( !stopDefaultListeners ) {
			@SuppressWarnings("unchecked")
			List<Class<?>> defaultListeners = (List<Class<?>>)
					reflectionManager.getDefaults().get( EntityListeners.class );

			if ( defaultListeners != null ) {
				int defaultListenerSize = defaultListeners.size();
				for ( int i = defaultListenerSize - 1; i >= 0; i-- ) {
					orderedListeners.add( defaultListeners.get( i ) );
				}
			}
		}

		for ( Class<?> listener : orderedListeners ) {
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

								Class<?> returnType = method.getReturnType();
								Class<?>[] args = method.getParameterTypes();
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

	public static List<CallbackDefinition> resolveEmbeddableCallbacks(
			ReflectionManager reflectionManager,
			Class<?> entityClass,
			Property embeddableProperty,
			CallbackType callbackType) {
		final Class<?> embeddableClass = embeddableProperty.getType().getReturnedClass();
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
							Class<?> returnType = method.getReturnType();
							Class<?>[] args = method.getParameterTypes();
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
					break;
				}
			}
		}
	}

	private static void applyListeners(ClassDetails currentClazz, List<Class<?>> listOfListeners) {
		final AnnotationUsage<EntityListeners> entityListeners = currentClazz.getAnnotationUsage( EntityListeners.class );
		if ( entityListeners != null ) {
			final List<ClassDetails> listeners = entityListeners.getList( "value" );
			for ( ClassDetails listener : listeners ) {
				listOfListeners.add( listener.toJavaClass() );
			}
		}

		if ( useAnnotationAnnotatedByListener ) {
			final List<AnnotationUsage<?>> metaAnnotatedUsageList = currentClazz.getMetaAnnotated( EntityListeners.class );
			for ( AnnotationUsage<?> metaAnnotatedUsage : metaAnnotatedUsageList ) {
				final AnnotationUsage<EntityListeners> metaAnnotatedListeners = metaAnnotatedUsage.getAnnotationDescriptor().getAnnotationUsage( EntityListeners.class );
				final List<ClassDetails> listeners = metaAnnotatedListeners.getList( "value" );
				for ( ClassDetails listener : listeners ) {
					listOfListeners.add( listener.toJavaClass() );
				}
			}
		}
	}
}
