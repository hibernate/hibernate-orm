/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.jpa.event.spi.PersistenceUnitCallbackType;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PostCreate;
import jakarta.persistence.PreClose;

/// Models persistence unit lifecycle callbacks declared by an `@EntityListener` class.
///
/// @author Gavin King
///
/// @since 8.0
public class PersistenceUnitLifecycleEventHandler {
	private final ClassDetails listenerClass;
	private final List<CallbackMethod> callbackMethods;

	public PersistenceUnitLifecycleEventHandler(
			@Nonnull ClassDetails listenerClass,
			@Nonnull List<CallbackMethod> callbackMethods) {
		this.listenerClass = listenerClass;
		this.callbackMethods = callbackMethods;
	}

	@Nonnull
	public ClassDetails getCallbackClass() {
		return listenerClass;
	}

	@Nonnull
	public List<CallbackMethod> getCallbackMethods() {
		return callbackMethods;
	}

	public boolean hasCallbackMethods() {
		return !callbackMethods.isEmpty();
	}

	@Nonnull
	public static PersistenceUnitLifecycleEventHandler from(@Nonnull ClassDetails listenerClassDetails) {
		final List<CallbackMethod> callbackMethods = new ArrayList<>();
		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			applyAnnotatedCallback(
					listenerClassDetails,
					methodDetails,
					PostCreate.class,
					PersistenceUnitCallbackType.POST_CREATE,
					callbackMethods
			);
			applyAnnotatedCallback(
					listenerClassDetails,
					methodDetails,
					PreClose.class,
					PersistenceUnitCallbackType.PRE_CLOSE,
					callbackMethods
			);
		} );
		return new PersistenceUnitLifecycleEventHandler( listenerClassDetails, callbackMethods );
	}

	@Internal
	@Nonnull
	public static PersistenceUnitLifecycleEventHandler from(
			@Nonnull ClassDetails listenerClassDetails,
			@Nonnull JaxbEntityListenerImpl jaxbMapping) {
		if ( !hasExplicitXmlCallbackMappings( jaxbMapping ) ) {
			return from( listenerClassDetails );
		}

		final List<CallbackMethod> callbackMethods = new ArrayList<>();
		applyXmlCallback(
				listenerClassDetails,
				jaxbMapping.getPostCreate(),
				PostCreate.class,
				PersistenceUnitCallbackType.POST_CREATE,
				callbackMethods
		);
		applyXmlCallback(
				listenerClassDetails,
				jaxbMapping.getPreClose(),
				PreClose.class,
				PersistenceUnitCallbackType.PRE_CLOSE,
				callbackMethods
		);
		return new PersistenceUnitLifecycleEventHandler( listenerClassDetails, callbackMethods );
	}

	public static boolean hasExplicitXmlCallbackMappings(@Nonnull JaxbEntityListenerImpl jaxbMapping) {
		return jaxbMapping.getPostCreate() != null
			|| jaxbMapping.getPreClose() != null;
	}

	private static void applyAnnotatedCallback(
			@Nonnull ClassDetails listenerClassDetails,
			@Nonnull MethodDetails methodDetails,
			@Nonnull Class<? extends Annotation> callbackAnnotation,
			@Nonnull PersistenceUnitCallbackType callbackType,
			@Nonnull List<CallbackMethod> callbackMethods) {
		if ( methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			validateSignature( listenerClassDetails, methodDetails, callbackAnnotation );
			checkDuplicate( listenerClassDetails, methodDetails, callbackType, callbackMethods );
			callbackMethods.add( new CallbackMethod( callbackType, methodDetails ) );
		}
	}

	private static void applyXmlCallback(
			@Nonnull ClassDetails listenerClassDetails,
			@Nullable JaxbLifecycleCallback lifecycleCallback,
			@Nonnull Class<? extends Annotation> callbackAnnotation,
			@Nonnull PersistenceUnitCallbackType callbackType,
			@Nonnull List<CallbackMethod> callbackMethods) {
		if ( lifecycleCallback != null ) {
			MethodDetails namedMethod = null;
			for ( var methodDetails : listenerClassDetails.getMethods() ) {
				if ( methodDetails.getName().equals( lifecycleCallback.getMethodName() ) ) {
					namedMethod = methodDetails;
					if ( hasValidSignature( methodDetails ) ) {
						checkDuplicate( listenerClassDetails, methodDetails, callbackType, callbackMethods );
						callbackMethods.add( new CallbackMethod( callbackType, methodDetails ) );
						return;
					}
				}
			}

			if ( namedMethod != null ) {
				validateSignature( listenerClassDetails, namedMethod, callbackAnnotation );
			}
			throw new ModelsException( "Persistence unit lifecycle callback method not found: "
										+ lifecycleCallback.getMethodName()
										+ " (" + listenerClassDetails.getClassName() + ")" );
		}
	}

	private static void validateSignature(
			@Nonnull ClassDetails listenerClassDetails,
			@Nonnull MethodDetails methodDetails,
			@Nonnull Class<? extends Annotation> callbackAnnotation) {
		if ( !hasValidSignature( methodDetails ) ) {
			throw new ModelsException( "Callback method '" + methodDetails.getName() + "' annotated '"
					+ callbackAnnotation.getName() + "' in '"
					+ listenerClassDetails.getClassName()
					+ "' must return void and have one parameter of type 'EntityAgent', 'EntityManager', or 'EntityManagerFactory'");
		}
	}

	private static boolean hasValidSignature(@Nonnull MethodDetails methodDetails) {
		return methodDetails.getArgumentTypes().size() == 1
			&& methodDetails.getReturnType() == ClassDetails.VOID_CLASS_DETAILS
			&& isPersistenceUnitLifecycleTarget( methodDetails.getArgumentTypes().get( 0 ) );
	}

	private static boolean isPersistenceUnitLifecycleTarget(@Nonnull ClassDetails argumentType) {
		final Class<?> javaClass = argumentType.toJavaClass();
		return javaClass == EntityManagerFactory.class
			|| javaClass == EntityManager.class
			|| javaClass == EntityAgent.class;
	}

	private static void checkDuplicate(
			@Nonnull ClassDetails listenerClassDetails,
			@Nonnull MethodDetails methodDetails,
			@Nonnull PersistenceUnitCallbackType callbackType,
			@Nonnull List<CallbackMethod> callbackMethods) {
		for ( var callbackMethod : callbackMethods ) {
			if ( callbackMethod.callbackType == callbackType
					&& hasSameParameterType( callbackMethod.methodDetails, methodDetails ) ) {
				throw new ModelsException( "You can only annotate one callback method per callback type and parameter type"
						+ " in callback class: " + listenerClassDetails.getClassName() );
			}
		}
	}

	private static boolean hasSameParameterType(@Nonnull MethodDetails first, @Nonnull MethodDetails second) {
		return first.getArgumentTypes().get( 0 ).toJavaClass()
				.equals( second.getArgumentTypes().get( 0 ).toJavaClass() );
	}

	public record CallbackMethod(
			PersistenceUnitCallbackType callbackType,
			MethodDetails methodDetails) {
	}
}
