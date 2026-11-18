package org.hibernate.boot.models.spi;

import org.hibernate.Remove;
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

import jakarta.annotation.Nullable;
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
import static org.hibernate.boot.models.JpaAnnotations.PRE_MERGE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_INSERT;
import static org.hibernate.boot.models.JpaAnnotations.POST_INSERT;
import static org.hibernate.boot.models.JpaAnnotations.PRE_UPSERT;
import static org.hibernate.boot.models.JpaAnnotations.POST_UPSERT;
import static org.hibernate.boot.models.JpaAnnotations.PRE_DELETE;
import static org.hibernate.boot.models.JpaAnnotations.POST_DELETE;

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
/// @see jakarta.persistence.PreMerge
/// @see jakarta.persistence.PreInsert
/// @see jakarta.persistence.PostInsert
/// @see jakarta.persistence.PreUpsert
/// @see jakarta.persistence.PostUpsert
/// @see jakarta.persistence.PreDelete
/// @see jakarta.persistence.PostDelete
///
/// @author Steve Ebersole
@Remove
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

	/// Create handlers from XML representation of a [callback][JpaEventListenerStyle#CALLBACK]
	/// or [listener][JpaEventListenerStyle#LISTENER].
	public static List<LifecycleEventHandler> from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping,
			ModelsContext modelsContext) {
		return from( consumerType, listenerClassDetails, jaxbMapping, modelsContext, true );
	}

	/// Create handlers from XML representation of a [callback][JpaEventListenerStyle#CALLBACK]
	/// or [listener][JpaEventListenerStyle#LISTENER].
	///
	/// Each callback element may match several overloads of the named method; every overload
	/// with a valid signature is registered, as a separate handler, provided no two methods
	/// handle the same event for the same parameter type.
	public static List<LifecycleEventHandler> from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping,
			ModelsContext modelsContext,
			boolean errorIfEmpty) {
		if ( isImplicitMethodMappings( jaxbMapping ) ) {
			if ( consumerType == JpaEventListenerStyle.LISTENER ) {
				// a listener class may have several callback methods for the same event,
				// provided they accept different parameter types
				return listenersForTarget( listenerClassDetails, null, errorIfEmpty );
			}
			final var handler = from( consumerType, listenerClassDetails, errorIfEmpty );
			return handler.hasCallbackMethods() ? List.of( handler ) : List.of();
		}

		final List<LifecycleEventHandler> lifecycleEventHandlers = new ArrayList<>();
		applyXmlCallbacks( jaxbMapping.getPrePersist(), consumerType, listenerClassDetails,
				PRE_PERSIST, CallbackType.PRE_PERSIST, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostPersist(), consumerType, listenerClassDetails,
				POST_PERSIST, CallbackType.POST_PERSIST, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreRemove(), consumerType, listenerClassDetails,
				PRE_REMOVE, CallbackType.PRE_REMOVE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostRemove(), consumerType, listenerClassDetails,
				POST_REMOVE, CallbackType.POST_REMOVE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreUpdate(), consumerType, listenerClassDetails,
				PRE_UPDATE, CallbackType.PRE_UPDATE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostUpdate(), consumerType, listenerClassDetails,
				POST_UPDATE, CallbackType.POST_UPDATE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostLoad(), consumerType, listenerClassDetails,
				POST_LOAD, CallbackType.POST_LOAD, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreMerge(), consumerType, listenerClassDetails,
				PRE_MERGE, CallbackType.PRE_MERGE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreInsert(), consumerType, listenerClassDetails,
				PRE_INSERT, CallbackType.PRE_INSERT, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostInsert(), consumerType, listenerClassDetails,
				POST_INSERT, CallbackType.POST_INSERT, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreUpsert(), consumerType, listenerClassDetails,
				PRE_UPSERT, CallbackType.PRE_UPSERT, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostUpsert(), consumerType, listenerClassDetails,
				POST_UPSERT, CallbackType.POST_UPSERT, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPreDelete(), consumerType, listenerClassDetails,
				PRE_DELETE, CallbackType.PRE_DELETE, modelsContext, lifecycleEventHandlers );
		applyXmlCallbacks( jaxbMapping.getPostDelete(), consumerType, listenerClassDetails,
				POST_DELETE, CallbackType.POST_DELETE, modelsContext, lifecycleEventHandlers );

		if ( errorIfEmpty && lifecycleEventHandlers.isEmpty() ) {
			throw new ModelsException( "Mapping for entity listener '"
										+ listenerClassDetails.getClassName()
										+ "' specified no callback methods" );
		}
		return lifecycleEventHandlers;
	}

	public static boolean hasExplicitXmlCallbackMappings(JaxbEntityListenerImpl jaxbMapping) {
		return !isImplicitMethodMappings( jaxbMapping );
	}

	// Registers the methods referenced by the XML callback elements mapped to a single
	// lifecycle event (e.g. all the `<pre-persist>` elements of an `<entity-listener>`).
	//
	// For each element, every method of the listener class named by its `method-name` and
	// having a valid callback signature for the given `consumerType` is:
	//
	// - registered as a separate LifecycleEventHandler for `callbackType`, and
	// - annotated with the corresponding callback annotation, so that the XML mapping is
	//   reflected in the models.
	//
	// A method already registered for `callbackType` (e.g. because several elements name the
	// same method) is skipped. Registering two different methods for the same event and the
	// same parameter type fails, see addIfAbsent.
	private static <A extends Annotation> void applyXmlCallbacks(
			List<? extends JaxbLifecycleCallback> lifecycleCallbacks,
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			AnnotationDescriptor<A> annotationDescriptor,
			CallbackType callbackType,
			ModelsContext modelsContext,
			List<LifecycleEventHandler> lifecycleEventHandlers) {
		for ( var lifecycleCallback : lifecycleCallbacks ) {
			final String methodName = lifecycleCallback.getMethodName();
			listenerClassDetails.forEachMethod( (index, methodDetails) -> {
				if ( methodDetails.getName().equals( methodName )
						&& matchesSignature( consumerType, methodDetails )
						&& !isRegistered( callbackType, methodDetails, lifecycleEventHandlers ) ) {
					addIfAbsent( listenerClassDetails, callbackType, methodDetails, lifecycleEventHandlers );
					( (MutableMemberDetails) methodDetails )
							.addAnnotationUsage( annotationDescriptor.createUsage( modelsContext ) );
				}
			} );
		}
	}

	// Whether the given method is already registered as the `callbackType` callback
	// of one of the given handlers.
	private static boolean isRegistered(
			CallbackType callbackType,
			MethodDetails methodDetails,
			List<LifecycleEventHandler> lifecycleEventHandlers) {
		for ( var existing : lifecycleEventHandlers ) {
			if ( existing.getCallbackMethod( callbackType ) == methodDetails ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isImplicitMethodMappings(JaxbEntityListenerImpl jaxbMapping) {
		return jaxbMapping.getPrePersist().isEmpty()
			&& jaxbMapping.getPreUpdate().isEmpty()
			&& jaxbMapping.getPreRemove().isEmpty()
			&& jaxbMapping.getPostLoad().isEmpty()
			&& jaxbMapping.getPostPersist().isEmpty()
			&& jaxbMapping.getPostUpdate().isEmpty()
			&& jaxbMapping.getPostRemove().isEmpty()
			&& jaxbMapping.getPreMerge().isEmpty()
			&& jaxbMapping.getPreInsert().isEmpty()
			&& jaxbMapping.getPostInsert().isEmpty()
			&& jaxbMapping.getPreUpsert().isEmpty()
			&& jaxbMapping.getPostUpsert().isEmpty()
			&& jaxbMapping.getPreDelete().isEmpty()
			&& jaxbMapping.getPostDelete().isEmpty();
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

	/// Create listener handlers from annotations, one per callback method, selecting only methods
	/// applicable to the callback target type, or all callback methods if the target type is `null`.
	public static List<LifecycleEventHandler> listenersForTarget(
			ClassDetails listenerClassDetails,
			@Nullable ClassDetails targetClassDetails,
			boolean errorIfEmpty) {
		final List<LifecycleEventHandler> lifecycleEventHandlers = new ArrayList<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PrePersist.class,
					CallbackType.PRE_PERSIST,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostPersist.class,
					CallbackType.POST_PERSIST,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreRemove.class,
					CallbackType.PRE_REMOVE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostRemove.class,
					CallbackType.POST_REMOVE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreMerge.class,
					CallbackType.PRE_MERGE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreInsert.class,
					CallbackType.PRE_INSERT,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostInsert.class,
					CallbackType.POST_INSERT,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreUpdate.class,
					CallbackType.PRE_UPDATE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostUpdate.class,
					CallbackType.POST_UPDATE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreUpsert.class,
					CallbackType.PRE_UPSERT,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostUpsert.class,
					CallbackType.POST_UPSERT,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PreDelete.class,
					CallbackType.PRE_DELETE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostDelete.class,
					CallbackType.POST_DELETE,
					lifecycleEventHandlers
			);
			applyTargetedAnnotatedCallback(
					listenerClassDetails,
					targetClassDetails,
					methodDetails,
					PostLoad.class,
					CallbackType.POST_LOAD,
					lifecycleEventHandlers
			);
		} );

		if ( errorIfEmpty && lifecycleEventHandlers.isEmpty() ) {
			throw new ModelsException( "Mapping for entity listener '"
										+ listenerClassDetails.getClassName()
										+ "' specified no callback methods" );
		}
		return lifecycleEventHandlers;
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
			@Nullable ClassDetails targetClassDetails,
			MethodDetails methodDetails,
			Class<? extends Annotation> callbackAnnotation,
			CallbackType callbackType,
			List<LifecycleEventHandler> lifecycleEventHandlers) {
		if ( methodDetails.hasDirectAnnotationUsage( callbackAnnotation ) ) {
			if ( !matchesSignature( JpaEventListenerStyle.LISTENER, methodDetails ) ) {
				throw new ModelsException( "Callback method '"
						+ methodDetails.getName() + "' annotated '@"
						+ callbackAnnotation.getSimpleName() + "' in '"
						+ listenerClassDetails.getClassName() + "'"
						+ signatureRequirement( JpaEventListenerStyle.LISTENER ) );
			}
			if ( targetClassDetails == null
					|| firstParameterType( methodDetails ).isAssignableFrom( targetClassDetails.toJavaClass() ) ) {
				addIfAbsent( listenerClassDetails, callbackType, methodDetails, lifecycleEventHandlers );
			}
		}
	}

	private static void addIfAbsent(
			ClassDetails listenerClassDetails,
			CallbackType callbackType,
			MethodDetails methodDetails,
			List<LifecycleEventHandler> lifecycleEventHandlers) {
		for ( var existing : lifecycleEventHandlers ) {
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
		lifecycleEventHandlers.add( withCallbackMethod( listenerClassDetails, callbackType, methodDetails ) );
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
