/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PreUpsert;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hibernate.boot.models.JpaAnnotations.POST_LOAD;
import static org.hibernate.boot.models.JpaAnnotations.POST_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.POST_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.POST_UPDATE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.PRE_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_UPDATE;

/// Models a JPA-style lifecycle event handler, whether [callback][JpaEventListenerStyle#CALLBACK] or
/// [listener][JpaEventListenerStyle#LISTENER] style.
/// It tracks event callback methods for a specific listener class, which may be the entity class or
/// mapped-superclass class in the case of [callback][JpaEventListenerStyle#CALLBACK] style.
///
/// @see jakarta.persistence.EntityListeners
/// @see jakarta.persistence.EntityListener
/// @see jakarta.persistence.PostLoad
/// @see jakarta.persistence.PostPersist
/// @see jakarta.persistence.PostRemove
/// @see jakarta.persistence.PostUpdate
/// @see jakarta.persistence.PrePersist
/// @see jakarta.persistence.PreRemove
/// @see jakarta.persistence.PreUpdate
///
/// @author Steve Ebersole
public class LifecycleEventHandler {

	private final JpaEventListenerStyle consumerType;
	private final ClassDetails listenerClass;
	private final Map<CallbackType, MethodDetails> callbackMethods;

	public LifecycleEventHandler(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClass,
			Map<CallbackType, MethodDetails> callbackMethods) {
		this.consumerType = consumerType;
		this.listenerClass = listenerClass;
		this.callbackMethods = new EnumMap<>( CallbackType.class );
		callbackMethods.forEach( (callbackType, method) -> {
			if ( method != null ) {
				this.callbackMethods.put( callbackType, method );
			}
		} );
	}

	public JpaEventListenerStyle getStyle() {
		return consumerType;
	}

	public ClassDetails getCallbackClass() {
		return listenerClass;
	}

	public MethodDetails getCallbackMethod(CallbackType callbackType) {
		return callbackMethods.get( callbackType );
	}

	public MethodDetails getPrePersistMethod() {
		return getCallbackMethod( CallbackType.PRE_PERSIST );
	}

	public MethodDetails getPostPersistMethod() {
		return getCallbackMethod( CallbackType.POST_PERSIST );
	}

	public MethodDetails getPreRemoveMethod() {
		return getCallbackMethod( CallbackType.PRE_REMOVE );
	}

	public MethodDetails getPostRemoveMethod() {
		return getCallbackMethod( CallbackType.POST_REMOVE );
	}

	public MethodDetails getPreUpdateMethod() {
		return getCallbackMethod( CallbackType.PRE_UPDATE );
	}

	public MethodDetails getPostUpdateMethod() {
		return getCallbackMethod( CallbackType.POST_UPDATE );
	}

	public MethodDetails getPostLoadMethod() {
		return getCallbackMethod( CallbackType.POST_LOAD );
	}

	public boolean hasCallbackMethods() {
		return !callbackMethods.isEmpty();
	}

	/// Create a handler from XML representation of a [callback][JpaEventListenerStyle#CALLBACK]
	/// or [listener][JpaEventListenerStyle#LISTENER].
	public static LifecycleEventHandler from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping,
			ModelsContext modelsContext) {
		return from( consumerType, listenerClassDetails, jaxbMapping, modelsContext, true );
	}

	/// Create a handler from XML representation of a [callback][JpaEventListenerStyle#CALLBACK]
	/// or [listener][JpaEventListenerStyle#LISTENER].
	public static LifecycleEventHandler from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping,
			ModelsContext modelsContext,
			boolean errorIfEmpty) {
		if ( isImplicitMethodMappings( jaxbMapping ) ) {
			return from( consumerType, listenerClassDetails, errorIfEmpty );
		}

		final EnumMap<CallbackType, MethodDetails> callbackMethods = new EnumMap<>( CallbackType.class );

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			final var mutableMethodDetails = (MutableMemberDetails) methodDetails;
			applyXmlCallback(
					jaxbMapping.getPrePersist(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PrePersist.class,
					PRE_PERSIST,
					CallbackType.PRE_PERSIST,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPostPersist(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PostPersist.class,
					POST_PERSIST,
					CallbackType.POST_PERSIST,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPreRemove(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PreRemove.class,
					PRE_REMOVE,
					CallbackType.PRE_REMOVE,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPostRemove(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PostRemove.class,
					POST_REMOVE,
					CallbackType.POST_REMOVE,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPreUpdate(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PreUpdate.class,
					PRE_UPDATE,
					CallbackType.PRE_UPDATE,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPostUpdate(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PostUpdate.class,
					POST_UPDATE,
					CallbackType.POST_UPDATE,
					modelsContext,
					callbackMethods
			);
			applyXmlCallback(
					jaxbMapping.getPostLoad(),
					methodDetails,
					mutableMethodDetails,
					consumerType,
					listenerClassDetails,
					PostLoad.class,
					POST_LOAD,
					CallbackType.POST_LOAD,
					modelsContext,
					callbackMethods
			);
		} );

		final var descriptor =
				new LifecycleEventHandler( consumerType, listenerClassDetails, callbackMethods );
		if ( errorIfEmpty ) {
			errorIfEmpty( descriptor );
		}
		return descriptor;
	}

	public static boolean hasExplicitXmlCallbackMappings(JaxbEntityListenerImpl jaxbMapping) {
		return !isImplicitMethodMappings( jaxbMapping );
	}

	private static <A extends Annotation> void applyXmlCallback(
			JaxbLifecycleCallback lifecycleCallback,
			MethodDetails methodDetails,
			MutableMemberDetails mutableMethodDetails,
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<A> callbackAnnotation,
			AnnotationDescriptor<A> annotationDescriptor,
			CallbackType callbackType,
			ModelsContext modelsContext,
			Map<CallbackType, MethodDetails> callbackMethods) {
		if ( lifecycleCallback != null
				&& methodDetails.getName().equals( lifecycleCallback.getMethodName() )
				&& setXmlCallbackMethod(
						consumerType,
						listenerClassDetails,
						callbackAnnotation,
						callbackType,
						methodDetails,
						callbackMethods ) ) {
			mutableMethodDetails.addAnnotationUsage( annotationDescriptor.createUsage( modelsContext ) );
		}
	}

	private static boolean isImplicitMethodMappings(JaxbEntityListenerImpl jaxbMapping) {
		return jaxbMapping.getPrePersist() == null
			&& jaxbMapping.getPreUpdate() == null
			&& jaxbMapping.getPreRemove() == null
			&& jaxbMapping.getPostLoad() == null
			&& jaxbMapping.getPostPersist() == null
			&& jaxbMapping.getPostUpdate() == null
			&& jaxbMapping.getPostRemove() == null;
	}

	private static void errorIfEmpty(LifecycleEventHandler descriptor) {
		if ( !descriptor.hasCallbackMethods() ) {
			throw new ModelsException( "Mapping for entity listener '"
										+ descriptor.listenerClass.getClassName()
										+ "' specified no callback methods" );
		}
	}

	/// Create a handler from annotations based on annotated methods on the given `listenerClassDetails`.
	/// Used from XML parsing when no explicit methods are identified indicating that we should use
	/// whatever event annotations are available, and errror if none are found.
	private static LifecycleEventHandler from(JpaEventListenerStyle consumerType, ClassDetails listenerClassDetails) {
		return from( consumerType, listenerClassDetails, true );
	}

	/// Create a handler from annotations based on annotated methods on the given `listenerClassDetails`.
	public static LifecycleEventHandler from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			boolean errorIfEmpty) {
		final EnumMap<CallbackType, MethodDetails> callbackMethods = new EnumMap<>( CallbackType.class );

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PrePersist.class,
					CallbackType.PRE_PERSIST,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostPersist.class,
					CallbackType.POST_PERSIST,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreRemove.class,
					CallbackType.PRE_REMOVE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostRemove.class,
					CallbackType.POST_REMOVE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreMerge.class,
					CallbackType.PRE_MERGE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreInsert.class,
					CallbackType.PRE_INSERT,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostInsert.class,
					CallbackType.POST_INSERT,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreUpdate.class,
					CallbackType.PRE_UPDATE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostUpdate.class,
					CallbackType.POST_UPDATE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreUpsert.class,
					CallbackType.PRE_UPSERT,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostUpsert.class,
					CallbackType.POST_UPSERT,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PreDelete.class,
					CallbackType.PRE_DELETE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostDelete.class,
					CallbackType.POST_DELETE,
					callbackMethods
			);
			applyAnnotatedCallback(
					consumerType,
					listenerClassDetails,
					methodDetails,
					PostLoad.class,
					CallbackType.POST_LOAD,
					callbackMethods
			);
		} );

		final var descriptor =
				new LifecycleEventHandler( consumerType, listenerClassDetails, callbackMethods );
		if ( errorIfEmpty ) {
			errorIfEmpty( descriptor );
		}
		return descriptor;
	}

	/// Create a listener handler from annotations, selecting only methods applicable to the callback target type.
	public static List<LifecycleEventHandler> listenersForTarget(
			ClassDetails listenerClassDetails,
			ClassDetails targetClassDetails,
			boolean errorIfEmpty) {
		final List<LifecycleEventHandler> descriptors = new ArrayList<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PrePersist.class,
					CallbackType.PRE_PERSIST,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostPersist.class,
					CallbackType.POST_PERSIST,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreRemove.class,
					CallbackType.PRE_REMOVE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostRemove.class,
					CallbackType.POST_REMOVE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreMerge.class,
					CallbackType.PRE_MERGE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreInsert.class,
					CallbackType.PRE_INSERT,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostInsert.class,
					CallbackType.POST_INSERT,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreUpdate.class,
					CallbackType.PRE_UPDATE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostUpdate.class,
					CallbackType.POST_UPDATE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreUpsert.class,
					CallbackType.PRE_UPSERT,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostUpsert.class,
					CallbackType.POST_UPSERT,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreDelete.class,
					CallbackType.PRE_DELETE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostDelete.class,
					CallbackType.POST_DELETE,
					descriptors
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostLoad.class,
					CallbackType.POST_LOAD,
					descriptors
			);
		} );

		if ( errorIfEmpty && descriptors.isEmpty() ) {
			throw new ModelsException( "Mapping for entity listener '"
										+ listenerClassDetails.getClassName()
										+ "' specified no callback methods" );
		}
		return descriptors;
	}

	private static void applyAnnotatedCallback(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			MethodDetails methodDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			Map<CallbackType, MethodDetails> callbackMethods) {
		if ( methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			setAnnotationCallbackMethod(
					consumerType,
					listenerClassDetails,
					callbackAnnotation,
					callbackType,
					methodDetails,
					callbackMethods
			);
		}
	}

	private static void applyTargetedAnnotatedCallback(
			ClassDetails listenerClassDetails,
			ClassDetails targetClassDetails,
			MethodDetails methodDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			List<LifecycleEventHandler> descriptors) {
		if ( methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			if ( !matchesSignature( JpaEventListenerStyle.LISTENER, methodDetails ) ) {
				throw new ModelsException( "Callback method '"
						+ methodDetails.getName() + "' annotated '@"
						+ callbackAnnotation.getSimpleName() + "' in '"
						+ listenerClassDetails.getClassName() + "'"
						+ signatureRequirement( JpaEventListenerStyle.LISTENER ) );
			}
			if ( firstParameterType( methodDetails )
					.isAssignableFrom( targetClassDetails.toJavaClass() ) ) {
				addIfAbsent( listenerClassDetails, callbackType, methodDetails, descriptors );
			}
		}
	}

	private static void addIfAbsent(
			ClassDetails listenerClassDetails,
			CallbackType callbackType,
			MethodDetails methodDetails,
			List<LifecycleEventHandler> descriptors) {
		for ( var existing : descriptors ) {
			final var existingMethod = existing.getCallbackMethod( callbackType );
			if ( existingMethod != null && existingMethod != methodDetails
					&& Objects.equals( firstParameterType( existingMethod ), firstParameterType( methodDetails ) ) ) {
				throw new ModelsException(
						"Multiple callback methods for callback type '@"
						+ callbackType.getCallbackAnnotation().getSimpleName()
						+ "' and parameter type '" + firstParameterType( methodDetails ).getSimpleName()
						+ "' in listener class '" + listenerClassDetails.getClassName() + "'" );
			}
		}
		descriptors.add( withCallbackMethod( listenerClassDetails, callbackType, methodDetails ) );
	}

	private static Class<?> firstParameterType(MethodDetails first) {
		return first.getArgumentTypes().get( 0 ).toJavaClass();
	}

	private static LifecycleEventHandler withCallbackMethod(
			ClassDetails listenerClassDetails,
			CallbackType callbackType,
			MethodDetails methodDetails) {
		final EnumMap<CallbackType, MethodDetails> callbackMethods = new EnumMap<>( CallbackType.class );
		callbackMethods.put( callbackType, methodDetails );
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails, callbackMethods );
	}

	private static boolean setXmlCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			MethodDetails methodDetails,
			Map<CallbackType, MethodDetails> callbackMethods) {
		if ( !matchesSignature( consumerType, methodDetails ) ) {
			return false;
		}
		else {
			setCallbackMethod(
					consumerType,
					listenerClassDetails,
					callbackAnnotation,
					callbackType,
					methodDetails,
					callbackMethods,
					"specified"
			);
			return true;
		}
	}

	private static void setAnnotationCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			MethodDetails methodDetails,
			Map<CallbackType, MethodDetails> callbackMethods) {
		setCallbackMethod(
				consumerType,
				listenerClassDetails,
				callbackAnnotation,
				callbackType,
				methodDetails,
				callbackMethods,
				"annotated"
		);
	}

	private static void setCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			MethodDetails methodDetails,
			Map<CallbackType, MethodDetails> callbackMethods,
			String source) {
		if ( !matchesSignature( consumerType, methodDetails ) ) {
			throw new ModelsException( "Callback method '"
					+ methodDetails.getName() + "' " + source + " '@"
					+ callbackAnnotation.getSimpleName() + "' in '"
					+ listenerClassDetails.getClassName() + "'"
					+ signatureRequirement( consumerType ) );
		}
		if ( callbackMethods.containsKey( callbackType ) ) {
			throw new ModelsException( "Duplicate callback method " + source + " '@"
					+ callbackAnnotation.getSimpleName() + "' in listener class '"
					+ listenerClassDetails.getClassName() + "'" );
		}
		callbackMethods.put( callbackType, methodDetails );
	}

	private static String signatureRequirement(JpaEventListenerStyle consumerType) {
		return switch ( consumerType ) {
			case CALLBACK -> " must return void and take no arguments";
			case LISTENER -> " must return void and take one argument";
		};
	}

	public static boolean matchesSignature(JpaEventListenerStyle callbackType, MethodDetails methodDetails) {
		return switch ( callbackType ) {
			case CALLBACK ->
				// should have no arguments, and technically (spec) have a void return
					methodDetails.getArgumentTypes().isEmpty()
						&& methodDetails.getReturnType() == ClassDetails.VOID_CLASS_DETAILS;
			case LISTENER ->
				// should have 1 argument, and technically (spec) have a void return
					methodDetails.getArgumentTypes().size() == 1
						&& methodDetails.getReturnType() == ClassDetails.VOID_CLASS_DETAILS;
		};
	}

}
