/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

	private final MethodDetails prePersistMethod;
	private final MethodDetails postPersistMethod;

	private final MethodDetails preRemoveMethod;
	private final MethodDetails postRemoveMethod;

	private final MethodDetails preUpdateMethod;
	private final MethodDetails postUpdateMethod;

	private final MethodDetails postLoadMethod;

	public LifecycleEventHandler(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClass,
			MethodDetails prePersistMethod,
			MethodDetails postPersistMethod,
			MethodDetails preRemoveMethod,
			MethodDetails postRemoveMethod,
			MethodDetails preUpdateMethod,
			MethodDetails postUpdateMethod,
			MethodDetails postLoadMethod) {
		this.consumerType = consumerType;
		this.listenerClass = listenerClass;
		this.prePersistMethod = prePersistMethod;
		this.postPersistMethod = postPersistMethod;
		this.preRemoveMethod = preRemoveMethod;
		this.postRemoveMethod = postRemoveMethod;
		this.preUpdateMethod = preUpdateMethod;
		this.postUpdateMethod = postUpdateMethod;
		this.postLoadMethod = postLoadMethod;
	}

	public JpaEventListenerStyle getStyle() {
		return consumerType;
	}

	public ClassDetails getCallbackClass() {
		return listenerClass;
	}

	public MethodDetails getPrePersistMethod() {
		return prePersistMethod;
	}

	public MethodDetails getPostPersistMethod() {
		return postPersistMethod;
	}

	public MethodDetails getPreRemoveMethod() {
		return preRemoveMethod;
	}

	public MethodDetails getPostRemoveMethod() {
		return postRemoveMethod;
	}

	public MethodDetails getPreUpdateMethod() {
		return preUpdateMethod;
	}

	public MethodDetails getPostUpdateMethod() {
		return postUpdateMethod;
	}

	public MethodDetails getPostLoadMethod() {
		return postLoadMethod;
	}

	public boolean hasCallbackMethods() {
		return prePersistMethod != null
			|| postPersistMethod != null
			|| preRemoveMethod != null
			|| postRemoveMethod != null
			|| preUpdateMethod != null
			|| postUpdateMethod != null
			|| postLoadMethod != null;
	}

	/// Create a handler from XML representation of a [callback][JpaEventListenerStyle#CALLBACK]
	/// or [listener][JpaEventListenerStyle#LISTENER].
	public static LifecycleEventHandler from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping,
			ModelsContext modelsContext) {
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		if ( isImplicitMethodMappings( jaxbMapping ) ) {
			return from( consumerType, listenerClassDetails );
		}

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			final MutableMemberDetails mutableMethodDetails = (MutableMemberDetails) methodDetails;
			if ( jaxbMapping.getPrePersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PrePersist.class, methodDetails, prePersistMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( PRE_PERSIST.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPostPersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostPersist().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PostPersist.class, methodDetails, postPersistMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( POST_PERSIST.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPreRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreRemove().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PreRemove.class, methodDetails, preRemoveMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( PRE_REMOVE.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPostRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostRemove().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PostRemove.class, methodDetails, postRemoveMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( POST_REMOVE.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPreUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpdate().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PreUpdate.class, methodDetails, preUpdateMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( PRE_UPDATE.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPostUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpdate().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PostUpdate.class, methodDetails, postUpdateMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( POST_UPDATE.createUsage( modelsContext ) );
				}
			}
			if ( jaxbMapping.getPostLoad() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostLoad().getMethodName() ) ) {
				if ( setXmlCallbackMethod( consumerType, listenerClassDetails, PostLoad.class, methodDetails, postLoadMethod ) ) {
					mutableMethodDetails.addAnnotationUsage( POST_LOAD.createUsage( modelsContext ) );
				}
			}
		} );

		final LifecycleEventHandler descriptor = new LifecycleEventHandler(
				consumerType,
				listenerClassDetails,
				prePersistMethod.get(),
				postPersistMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				postLoadMethod.get()
		);

		errorIfEmpty( descriptor );

		return descriptor;
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
			throw new ModelsException( "Mapping for entity-listener specified no callback methods - " + descriptor.listenerClass.getClassName() );
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
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PrePersist.class, prePersistMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PreInsert.class, prePersistMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostPersist.class, postPersistMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostInsert.class, postPersistMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PreRemove.class, preRemoveMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PreDelete.class, preRemoveMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostRemove.class, postRemoveMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostDelete.class, postRemoveMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PreUpdate.class, preUpdateMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostUpdate.class, postUpdateMethod );
			applyAnnotatedCallback( consumerType, listenerClassDetails, methodDetails, PostLoad.class, postLoadMethod );
		} );

		final LifecycleEventHandler descriptor = new LifecycleEventHandler(
				consumerType,
				listenerClassDetails,
				prePersistMethod.get(),
				postPersistMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				postLoadMethod.get()
		);
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
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PrePersist.class,
					(method) -> withPrePersistMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PreInsert.class,
					(method) -> withPrePersistMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostPersist.class,
					(method) -> withPostPersistMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostInsert.class,
					(method) -> withPostPersistMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PreRemove.class,
					(method) -> withPreRemoveMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PreDelete.class,
					(method) -> withPreRemoveMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostRemove.class,
					(method) -> withPostRemoveMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostDelete.class,
					(method) -> withPostRemoveMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PreUpdate.class,
					(method) -> withPreUpdateMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostUpdate.class,
					(method) -> withPostUpdateMethod( listenerClassDetails, method ), descriptors );
			applyTargetedAnnotatedCallback( listenerClassDetails, targetClassDetails, methodDetails, PostLoad.class,
					(method) -> withPostLoadMethod( listenerClassDetails, method ), descriptors );
		} );

		if ( errorIfEmpty && descriptors.isEmpty() ) {
			throw new ModelsException( "Mapping for entity-listener specified no callback methods - "
					+ listenerClassDetails.getClassName() );
		}
		return descriptors;
	}

	private static void applyAnnotatedCallback(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			MethodDetails methodDetails,
			Class<? extends Annotation> callbackAnnotation,
			MutableObject<MethodDetails> callbackMethod) {
		if ( methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			setAnnotationCallbackMethod( consumerType, listenerClassDetails, callbackAnnotation, methodDetails, callbackMethod );
		}
	}

	private static void applyTargetedAnnotatedCallback(
			ClassDetails listenerClassDetails,
			ClassDetails targetClassDetails,
			MethodDetails methodDetails,
			Class<? extends Annotation> callbackAnnotation,
			Function<MethodDetails, LifecycleEventHandler> descriptorCreator,
			List<LifecycleEventHandler> descriptors) {
		if ( !methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			return;
		}
		if ( !matchesSignature( JpaEventListenerStyle.LISTENER, methodDetails ) ) {
			throw new ModelsException( "Callback methods annotated for "
					+ callbackAnnotation.getName() + " in "
					+ listenerClassDetails.getClassName()
					+ signatureRequirement( JpaEventListenerStyle.LISTENER )
					+ ": " + methodDetails );
		}
		if ( methodDetails.getArgumentTypes().get( 0 ).toJavaClass().isAssignableFrom( targetClassDetails.toJavaClass() ) ) {
			addIfAbsent( descriptorCreator.apply( methodDetails ), descriptors );
		}
	}

	private static void addIfAbsent(LifecycleEventHandler descriptor, List<LifecycleEventHandler> descriptors) {
		for ( LifecycleEventHandler existing : descriptors ) {
			if ( sameCallbackMethod( existing, descriptor ) ) {
				return;
			}
		}
		descriptors.add( descriptor );
	}

	private static boolean sameCallbackMethod(LifecycleEventHandler first, LifecycleEventHandler second) {
		return first.prePersistMethod == second.prePersistMethod
			&& first.postPersistMethod == second.postPersistMethod
			&& first.preRemoveMethod == second.preRemoveMethod
			&& first.postRemoveMethod == second.postRemoveMethod
			&& first.preUpdateMethod == second.preUpdateMethod
			&& first.postUpdateMethod == second.postUpdateMethod
			&& first.postLoadMethod == second.postLoadMethod;
	}

	private static LifecycleEventHandler withPrePersistMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				methodDetails,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static LifecycleEventHandler withPostPersistMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				methodDetails,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static LifecycleEventHandler withPreRemoveMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				null,
				methodDetails,
				null,
				null,
				null,
				null
		);
	}

	private static LifecycleEventHandler withPostRemoveMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				null,
				null,
				methodDetails,
				null,
				null,
				null
		);
	}

	private static LifecycleEventHandler withPreUpdateMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				null,
				null,
				null,
				methodDetails,
				null,
				null
		);
	}

	private static LifecycleEventHandler withPostUpdateMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				null,
				null,
				null,
				null,
				methodDetails,
				null
		);
	}

	private static LifecycleEventHandler withPostLoadMethod(ClassDetails listenerClassDetails, MethodDetails methodDetails) {
		return new LifecycleEventHandler( JpaEventListenerStyle.LISTENER, listenerClassDetails,
				null,
				null,
				null,
				null,
				null,
				null,
				methodDetails
		);
	}

	private static boolean setXmlCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			MethodDetails methodDetails,
			MutableObject<MethodDetails> callbackMethod) {
		if ( !matchesSignature( consumerType, methodDetails ) ) {
			return false;
		}
		setCallbackMethod( consumerType, listenerClassDetails, callbackAnnotation, methodDetails, callbackMethod, "specified" );
		return true;
	}

	private static void setAnnotationCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			MethodDetails methodDetails,
			MutableObject<MethodDetails> callbackMethod) {
		setCallbackMethod( consumerType, listenerClassDetails, callbackAnnotation, methodDetails, callbackMethod, "annotated" );
	}

	private static void setCallbackMethod(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			Class<? extends Annotation> callbackAnnotation,
			MethodDetails methodDetails,
			MutableObject<MethodDetails> callbackMethod,
			String source) {
		if ( !matchesSignature( consumerType, methodDetails ) ) {
			throw new ModelsException( "Callback methods " + source + " for "
					+ callbackAnnotation.getName() + " in "
					+ listenerClassDetails.getClassName()
					+ signatureRequirement( consumerType )
					+ ": " + methodDetails );
		}
		if ( callbackMethod.get() != null ) {
			throw new ModelsException( "You can only " + source + " one callback method for "
					+ callbackAnnotation.getName() + " in callback class: "
					+ listenerClassDetails.getClassName() );
		}
		callbackMethod.set( methodDetails );
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
