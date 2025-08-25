/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.property.access.spi.Getter;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Resolves JPA callback definitions
 *
 * @author Steve Ebersole
 */
public final class CallbackDefinitionResolver {

	private static List<CallbackDefinition> resolveEntityCallbacks(
			InFlightMetadataCollector metadataCollector,
			ClassDetails entityClass,
			CallbackType callbackType) {
		final ModelsContext modelsContext = metadataCollector.getBootstrapContext().getModelsContext();

		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		final List<ClassDetails> orderedListeners = new ArrayList<>();

		ClassDetails currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;

		do {
			CallbackDefinition callbackDefinition = null;
			final List<MethodDetails> methodsDetailsList = currentClazz.getMethods();
			for ( MethodDetails methodDetails : methodsDetailsList ) {
				if ( methodDetails.hasDirectAnnotationUsage( callbackType.getCallbackAnnotation() )
						&& !callbacksMethodNames.contains( methodDetails.getName() ) ) {
					//overridden method, remove the superclass overridden method
					if ( callbackDefinition == null ) {
						final Method javaMethod = (Method) methodDetails.toJavaMember();
						callbackDefinition = new EntityCallback.Definition( javaMethod, callbackType );
						final Class<?> returnType = javaMethod.getReturnType();
						final Class<?>[] args = javaMethod.getParameterTypes();
						if ( returnType != Void.TYPE || args.length != 0 ) {
							throw new RuntimeException(
									"Callback methods annotated on the bean class must return void and take no arguments: "
									+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
							);
						}
						ReflectHelper.ensureAccessibility( javaMethod );
						callbackDefinitions.add( 0, callbackDefinition ); //superclass first
						callbacksMethodNames.add( 0, methodDetails.getName() );
					}
					else {
						throw new PersistenceException(
								"You can only annotate one callback method with "
								+ callbackType.getCallbackAnnotation()
										.getName() + " in bean class: " + entityClass.getName()
						);
					}
				}

			}
			if ( !stopListeners ) {
				applyListeners( currentClazz, orderedListeners, modelsContext );
				stopListeners = currentClazz.hasDirectAnnotationUsage( ExcludeSuperclassListeners.class );
				stopDefaultListeners = currentClazz.hasDirectAnnotationUsage( ExcludeDefaultListeners.class );
			}

			do {
				currentClazz = currentClazz.getSuperClass();
			}
			while ( currentClazz != null
					&& !( currentClazz.hasDirectAnnotationUsage( Entity.class )
						|| currentClazz.hasDirectAnnotationUsage( MappedSuperclass.class ) ) );
		}
		while ( currentClazz != null );

		//handle default listeners
		if ( !stopDefaultListeners ) {
			final List<JpaEventListener> globalListenerRegistrations =
					metadataCollector.getGlobalRegistrations().getEntityListenerRegistrations();
			if ( isNotEmpty( globalListenerRegistrations ) ) {
				int defaultListenerSize = globalListenerRegistrations.size();
				for ( int i = defaultListenerSize - 1; i >= 0; i-- ) {
					orderedListeners.add( globalListenerRegistrations.get( i ).getCallbackClass() );
				}
			}
		}

		for ( ClassDetails listenerClassDetails : orderedListeners ) {
			CallbackDefinition callbackDefinition = null;
			if ( listenerClassDetails != null ) {
				for ( MethodDetails methodDetails : listenerClassDetails.getMethods() ) {
					if ( methodDetails.hasDirectAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
						//overridden method, remove the superclass overridden method
						if ( callbackDefinition == null ) {
							final Method method = (Method) methodDetails.toJavaMember();
							final Class<?> listenerClass = listenerClassDetails.toJavaClass();
							callbackDefinition = new ListenerCallback.Definition( listenerClass, method, callbackType );

							final Class<?> returnType = method.getReturnType();
							final Class<?>[] args = method.getParameterTypes();
							if ( returnType != Void.TYPE || args.length != 1 ) {
								throw new PersistenceException(
										"Callback methods annotated in a listener bean class must return void and take one argument: "
												+ callbackType.getCallbackAnnotation().getName() + " - " + methodDetails
								);
							}
							ReflectHelper.ensureAccessibility( method );
							callbackDefinitions.add( 0, callbackDefinition ); // listeners first
						}
						else {
							throw new PersistenceException(
									"You can only annotate one callback method with "
											+ callbackType.getCallbackAnnotation().getName()
											+ " in bean class: " + entityClass.getName()
											+ " and callback listener: " + listenerClassDetails.getName()
							);
						}
					}
				}
			}
		}
		return callbackDefinitions;
	}

	/**
	 * @deprecated See discussion in {@link EmbeddableCallback}.
	 */
	@Deprecated(since = "7")
	private static List<CallbackDefinition> resolveEmbeddableCallbacks(
			InFlightMetadataCollector metadataCollector,
			Class<?> entityClass,
			Property embeddableProperty,
			CallbackType callbackType) {

		final ModelsContext modelsContext = metadataCollector.getBootstrapContext().getModelsContext();
		final Class<?> embeddableClass = embeddableProperty.getType().getReturnedClass();
		final ClassDetails embeddableClassDetails = modelsContext.getClassDetailsRegistry().getClassDetails( embeddableClass.getName() );

		final Getter embeddableGetter = embeddableProperty.getGetter( entityClass );
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		ClassDetails currentClass = embeddableClassDetails;
		do {
			CallbackDefinition callbackDefinition = null;
			final List<MethodDetails> methodsDetailsList = currentClass.getMethods();
			for ( MethodDetails methodDetails : methodsDetailsList ) {
				if ( methodDetails.hasDirectAnnotationUsage( callbackType.getCallbackAnnotation() ) ) {
					final Method method = methodDetails.toJavaMember();
					final String methodName = method.getName();
					final String callbackName = callbackType.getCallbackAnnotation().getName();
					final String currentClassName = currentClass.getName();

					DEPRECATION_LOGGER.embeddableLifecycleCallback( callbackName, currentClassName );

					if ( callbacksMethodNames.contains( methodName ) ) {
						throw new PersistenceException( "Multiple callback methods annotated '@" + callbackName
														+ "' in bean class '" + currentClassName + "'" );
					}

					//overridden method, remove the superclass overridden method
					if ( callbackDefinition == null ) {
						callbackDefinition = new EmbeddableCallback.Definition( embeddableGetter, method, callbackType );
						final Class<?> returnType = method.getReturnType();
						final Class<?>[] args = method.getParameterTypes();
						if ( returnType != Void.TYPE || args.length != 0 ) {
							throw new RuntimeException(
									"Callback methods annotated on the bean class must return void and take no arguments: "
									+ callbackName + " - " + methodDetails
							);
						}
						ReflectHelper.ensureAccessibility( method );
						callbackDefinitions.add( 0, callbackDefinition ); //superclass first
						callbacksMethodNames.add( 0, methodName );
					}
				}

			}

			do {
				currentClass = currentClass.getSuperClass();
			}
			while ( currentClass != null && !currentClass.hasDirectAnnotationUsage( MappedSuperclass.class ) );
		}
		while ( currentClass != null );

		return callbackDefinitions;
	}

	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		final var target = EntityListeners.class.getAnnotation( Target.class );
		if ( target != null ) {
			for ( ElementType type : target.value() ) {
				if ( type.equals( ElementType.ANNOTATION_TYPE ) ) {
					useAnnotationAnnotatedByListener = true;
					break;
				}
			}
		}
	}

	private static void applyListeners(
			ClassDetails currentClazz,
			List<ClassDetails> listOfListeners,
			ModelsContext sourceModelContext) {
		final var classDetailsRegistry = sourceModelContext.getClassDetailsRegistry();

		final var entityListeners = currentClazz.getDirectAnnotationUsage( EntityListeners.class );
		if ( entityListeners != null ) {
			final var listenerClasses = entityListeners.value();
			int size = listenerClasses.length;
			for ( int index = size - 1; index >= 0; index-- ) {
				listOfListeners.add( classDetailsRegistry.resolveClassDetails( listenerClasses[index].getName() ) );
			}
		}

		if ( useAnnotationAnnotatedByListener ) {
			for ( var metaAnnotatedUsage : currentClazz.getMetaAnnotated( EntityListeners.class, sourceModelContext ) ) {
				final var descriptor =
						sourceModelContext.getAnnotationDescriptorRegistry()
								.getDescriptor( metaAnnotatedUsage.getClass() );
				final var listenerClasses = descriptor.getDirectAnnotationUsage( EntityListeners.class ).value();
				for ( int index = listenerClasses.length - 1; index >= 0; index-- ) {
					listOfListeners.add( classDetailsRegistry.resolveClassDetails( listenerClasses[index].getName() ) );
				}
			}
		}
	}

	/**
	 * See {@link JpaEventListener} for a better (?) alternative
	 */
	public static void resolveLifecycleCallbacks(
			ClassDetails entityClass,
			PersistentClass persistentClass,
			InFlightMetadataCollector collector) {
		for ( CallbackType callbackType : CallbackType.values() ) {
			persistentClass.addCallbackDefinitions( resolveEntityCallbacks( collector, entityClass, callbackType ) );
		}

		// Note: @Embeddable classes are not supposed to have entity callbacks according to
		//       the JPA specification, and it doesn't even really make sense to allow them
		//       to, since they don't have a well-defined "lifecycle", but unfortunately this
		//       code was added by HHH-12326
		collector.addSecondPass( persistentClasses -> {
			for ( Property property : persistentClass.getDeclaredProperties() ) {
			if ( property.getValue() instanceof Component component
					// embedded components don't have their own class, so no need to check callbacks (see HHH-19671)
					&& !component.isEmbedded() ) {
					try {
						final Class<?> mappedClass = persistentClass.getMappedClass();
						for ( CallbackType type : CallbackType.values() ) {
							property.addCallbackDefinitions( resolveEmbeddableCallbacks( collector, mappedClass, property, type ) );
						}
					}
					catch (ClassLoadingException ignore) {
						// a dynamic embeddable... cannot define listener methods
					}
				}
			}
		} );
	}
}
