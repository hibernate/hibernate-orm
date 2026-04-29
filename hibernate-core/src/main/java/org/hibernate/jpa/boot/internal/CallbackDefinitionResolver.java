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
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.boot.models.spi.LifecycleEventHandler;
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
import java.util.Map;

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
		final List<LifecycleEventHandler> orderedListeners = new ArrayList<>();
		final List<LifecycleEventHandler> orderedDefaultListeners = new ArrayList<>();

		ClassDetails currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;

		do {
			final LifecycleEventHandler callbackRegistration = LifecycleEventHandler.from(
					JpaEventListenerStyle.CALLBACK,
					currentClazz,
					false
			);
			final MethodDetails callbackMethod = getCallbackMethod( callbackRegistration, callbackType );
			if ( callbackMethod != null && !callbacksMethodNames.contains( callbackMethod.getName() ) ) {
				final Method javaMethod = (Method) callbackMethod.toJavaMember();
				ReflectHelper.ensureAccessibility( javaMethod );
				callbackDefinitions.add( 0, new EntityCallbackDefinition( javaMethod, callbackType ) ); //superclass first
				callbacksMethodNames.add( 0, callbackMethod.getName() );
			}

			if ( !stopListeners ) {
				applyListeners( currentClazz, entityClass, orderedListeners, modelsContext );
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
			final List<LifecycleEventHandler> globalListenerRegistrations = new ArrayList<>(
					metadataCollector.getGlobalRegistrations().getEntityListenerRegistrations()
			);
			collectTargetedListenerRegistrations( metadataCollector, entityClass, globalListenerRegistrations );
			if ( isNotEmpty( globalListenerRegistrations ) ) {
				int defaultListenerSize = globalListenerRegistrations.size();
				for ( int i = defaultListenerSize - 1; i >= 0; i-- ) {
					orderedDefaultListeners.add( globalListenerRegistrations.get( i ) );
				}
			}
		}

		for ( LifecycleEventHandler listenerRegistration : orderedListeners ) {
			final CallbackDefinition callbackDefinition = resolveListenerCallback(
					listenerRegistration,
					entityClass,
					callbackType
			);
			if ( callbackDefinition != null ) {
				callbackDefinitions.add( 0, callbackDefinition ); // listeners first
			}
		}
		for ( LifecycleEventHandler listenerRegistration : orderedDefaultListeners ) {
			final CallbackDefinition callbackDefinition = resolveListenerCallback(
					listenerRegistration,
					entityClass,
					callbackType
			);
			if ( callbackDefinition != null ) {
				callbackDefinitions.add( 0, callbackDefinition );
			}
		}
		return callbackDefinitions;
	}

	private static CallbackDefinition resolveListenerCallback(
			LifecycleEventHandler listenerRegistration,
			ClassDetails entityClass,
			CallbackType callbackType) {
		final MethodDetails callbackMethod = getCallbackMethod( listenerRegistration, callbackType );

		if ( callbackMethod == null ) {
			return null;
		}
		if ( !isCompatibleCallbackTarget( callbackMethod, entityClass ) ) {
			return null;
		}

		final Method method = (Method) callbackMethod.toJavaMember();
		ReflectHelper.ensureAccessibility( method );
		return new ListenerCallbackDefinition(
				listenerRegistration.getCallbackClass().toJavaClass(),
				method,
				callbackType
		);
	}

	private static boolean isCompatibleCallbackTarget(MethodDetails callbackMethod, ClassDetails entityClass) {
		return callbackMethod.getArgumentTypes().get( 0 ).toJavaClass().isAssignableFrom( entityClass.toJavaClass() );
	}

	private static void collectTargetedListenerRegistrations(
			InFlightMetadataCollector metadataCollector,
			ClassDetails entityClass,
			List<LifecycleEventHandler> globalListenerRegistrations) {
		final Map<ClassDetails, List<LifecycleEventHandler>> targetedRegistrations =
				metadataCollector.getGlobalRegistrations().getTargetedEntityListenerRegistrations();
		if ( targetedRegistrations.isEmpty() ) {
			return;
		}

		final Class<?> entityJavaClass = entityClass.toJavaClass();
		targetedRegistrations.forEach( (targetClass, listenerRegistrations) -> {
			if ( targetClass.toJavaClass().isAssignableFrom( entityJavaClass ) ) {
				globalListenerRegistrations.addAll( listenerRegistrations );
			}
		} );
	}

	private static MethodDetails getCallbackMethod(
			LifecycleEventHandler listenerRegistration,
			CallbackType callbackType) {
		return switch ( callbackType ) {
			case PRE_PERSIST -> listenerRegistration.getPrePersistMethod();
			case POST_PERSIST -> listenerRegistration.getPostPersistMethod();
			case PRE_REMOVE -> listenerRegistration.getPreRemoveMethod();
			case POST_REMOVE -> listenerRegistration.getPostRemoveMethod();
			case PRE_UPDATE -> listenerRegistration.getPreUpdateMethod();
			case POST_UPDATE -> listenerRegistration.getPostUpdateMethod();
			case POST_LOAD -> listenerRegistration.getPostLoadMethod();
		};
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
			ClassDetails entityClass,
			List<LifecycleEventHandler> listOfListeners,
			ModelsContext sourceModelContext) {
		final var classDetailsRegistry = sourceModelContext.getClassDetailsRegistry();

		final var entityListeners = currentClazz.getDirectAnnotationUsage( EntityListeners.class );
		if ( entityListeners != null ) {
			final var listenerClasses = entityListeners.value();
			int size = listenerClasses.length;
			for ( int index = size - 1; index >= 0; index-- ) {
				applyListener(
						classDetailsRegistry.resolveClassDetails( listenerClasses[index].getName() ),
						entityClass,
						listOfListeners
				);
			}
		}

		if ( useAnnotationAnnotatedByListener ) {
			for ( var metaAnnotatedUsage : currentClazz.getMetaAnnotated( EntityListeners.class, sourceModelContext ) ) {
				final var descriptor =
						sourceModelContext.getAnnotationDescriptorRegistry()
								.getDescriptor( metaAnnotatedUsage.getClass() );
				final var listenerClasses = descriptor.getDirectAnnotationUsage( EntityListeners.class ).value();
				for ( int index = listenerClasses.length - 1; index >= 0; index-- ) {
					applyListener(
							classDetailsRegistry.resolveClassDetails( listenerClasses[index].getName() ),
							entityClass,
							listOfListeners
					);
				}
			}
		}
	}

	private static void applyListener(
			ClassDetails listenerClassDetails,
			ClassDetails entityClass,
			List<LifecycleEventHandler> listOfListeners) {
		final List<LifecycleEventHandler> eventListeners = LifecycleEventHandler.listenersForTarget(
				listenerClassDetails,
				entityClass,
				false
		);
		listOfListeners.addAll( eventListeners );
	}

	/**
	 * Uses {@link LifecycleEventHandler} for listener descriptors while retaining the JPA hierarchy
	 * and exclusion rules handled here.
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
