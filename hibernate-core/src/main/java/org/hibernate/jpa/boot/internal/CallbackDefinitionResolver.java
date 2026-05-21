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
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
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
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.boot.models.spi.LifecycleEventHandler.listenersForTarget;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualifier;

/// Resolves JPA callback definitions
///
/// @author Steve Ebersole
public final class CallbackDefinitionResolver {

	private static List<CallbackDefinition> resolveEntityCallbacks(
			InFlightMetadataCollector metadataCollector,
			ClassDetails entityClass,
			CallbackType callbackType) {

		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();
		final List<String> callbacksMethodNames = new ArrayList<>();
		final List<LifecycleEventHandler> orderedPackageListeners = new ArrayList<>();
		final List<LifecycleEventHandler> orderedListeners = new ArrayList<>();
		final List<LifecycleEventHandler> orderedDefaultListeners = new ArrayList<>();

		ClassDetails currentClazz = entityClass;
		boolean stopListeners = false;
		boolean stopDefaultListeners = false;

		final var modelsContext = metadataCollector.getBootstrapContext().getModelsContext();
		do {
			final var callbackRegistration =
					LifecycleEventHandler.from( JpaEventListenerStyle.CALLBACK, currentClazz, false );
			final var callbackMethod = callbackRegistration.getCallbackMethod( callbackType );
			if ( callbackMethod != null && !callbacksMethodNames.contains( callbackMethod.getName() ) ) {
				final var javaMethod = callbackMethod.toJavaMember();
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

		applyPackageListeners( entityClass, orderedPackageListeners, modelsContext );

		//handle default listeners
		if ( !stopDefaultListeners ) {
			final var globalListenerRegistrations = new ArrayList<>(
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

		for ( var listenerRegistration : orderedListeners ) {
			final var callbackDefinition =
					resolveListenerCallback( listenerRegistration, entityClass, callbackType );
			if ( callbackDefinition != null ) {
				callbackDefinitions.add( 0, callbackDefinition ); // listeners first
			}
		}
		for ( var listenerRegistration : orderedPackageListeners ) {
			final var callbackDefinition =
					resolveListenerCallback( listenerRegistration, entityClass, callbackType );
			if ( callbackDefinition != null ) {
				callbackDefinitions.add( 0, callbackDefinition );
			}
		}
		for ( var listenerRegistration : orderedDefaultListeners ) {
			final var callbackDefinition =
					resolveListenerCallback( listenerRegistration, entityClass, callbackType );
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
		final var callbackMethod = listenerRegistration.getCallbackMethod( callbackType );
		if ( callbackMethod == null ) {
			return null;
		}
		else if ( !isCompatibleCallbackTarget( callbackMethod, entityClass ) ) {
			return null;
		}
		else {
			final var method = callbackMethod.toJavaMember();
			ReflectHelper.ensureAccessibility( method );
			return new ListenerCallbackDefinition(
					listenerRegistration.getCallbackClass().toJavaClass(),
					method,
					callbackType
			);
		}
	}

	private static boolean isCompatibleCallbackTarget(MethodDetails callbackMethod, ClassDetails entityClass) {
		return callbackMethod.getArgumentTypes().get( 0 ).toJavaClass().isAssignableFrom( entityClass.toJavaClass() );
	}

	private static void collectTargetedListenerRegistrations(
			InFlightMetadataCollector metadataCollector,
			ClassDetails entityClass,
			List<LifecycleEventHandler> globalListenerRegistrations) {
		final var targetedRegistrations =
				metadataCollector.getGlobalRegistrations()
						.getTargetedEntityListenerRegistrations();
		if ( !targetedRegistrations.isEmpty() ) {
			final var entityJavaClass = entityClass.toJavaClass();
			targetedRegistrations.forEach( (targetClass, listenerRegistrations) -> {
				if ( targetClass.toJavaClass().isAssignableFrom( entityJavaClass ) ) {
					globalListenerRegistrations.addAll( listenerRegistrations );
				}
			} );
		}
	}

	private static boolean useAnnotationAnnotatedByListener;

	static {
		//check whether reading annotations of annotations is useful or not
		useAnnotationAnnotatedByListener = false;
		final var target = EntityListeners.class.getAnnotation( Target.class );
		if ( target != null ) {
			for ( var type : target.value() ) {
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

	private static void applyPackageListeners(
			ClassDetails entityClass,
			List<LifecycleEventHandler> listOfListeners,
			ModelsContext sourceModelContext) {
		final String packageName = qualifier( entityClass.getName() );
		if ( !isEmpty( packageName ) ) {
			try {
				applyListeners(
						sourceModelContext.getClassDetailsRegistry()
								.resolveClassDetails( packageName + ".package-info" ),
						entityClass,
						listOfListeners,
						sourceModelContext
				);
			}
			catch (ClassLoadingException ignore) {
			}
		}
	}

	private static void applyListener(
			ClassDetails listenerClassDetails,
			ClassDetails entityClass,
			List<LifecycleEventHandler> listOfListeners) {
		listOfListeners.addAll( listenersForTarget( listenerClassDetails, entityClass, false ) );
	}

	/**
	 * Uses {@link LifecycleEventHandler} for listener descriptors while retaining the JPA hierarchy
	 * and exclusion rules handled here.
	 */
	public static void resolveLifecycleCallbacks(
			ClassDetails entityClass,
			PersistentClass persistentClass,
			InFlightMetadataCollector collector) {
		for ( var callbackType : CallbackType.values() ) {
			persistentClass.addCallbackDefinitions( resolveEntityCallbacks( collector, entityClass, callbackType ) );
		}
	}
}
