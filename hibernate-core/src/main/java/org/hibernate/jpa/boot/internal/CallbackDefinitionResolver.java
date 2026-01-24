/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import org.hibernate.boot.models.spi.JpaEventListener;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.boot.spi.CallbackDefinition;
import org.hibernate.jpa.boot.spi.EntityCallbackDefinition;
import org.hibernate.jpa.boot.spi.ListenerCallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/// Resolves JPA callback definitions
///
/// @author Steve Ebersole
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
						callbackDefinition = new EntityCallbackDefinition( javaMethod, callbackType );
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
							callbackDefinition = new ListenerCallbackDefinition( listenerClass, method, callbackType );

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
	}
}
