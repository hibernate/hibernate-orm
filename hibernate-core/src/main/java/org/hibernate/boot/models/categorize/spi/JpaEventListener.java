/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/// Categorized JPA lifecycle callback descriptor.
///
/// A listener may represent callback methods declared directly on a managed type or
/// listener methods declared on a separate listener class.  XML mappings identify
/// callback methods by name, while annotations identify them by lifecycle callback
/// annotations.
///
/// @see JaxbPersistenceUnitDefaultsImpl#getEntityListenerContainer()
/// @see JaxbEntityListenerImpl
/// @see GlobalRegistrations#getEntityListenerRegistrations()
///
/// @since 9.0
/// @author Steve Ebersole
public class JpaEventListener {

	private final JpaEventListenerStyle consumerType;
	private final ClassDetails listenerClass;

	private final MethodDetails prePersistMethod;
	private final MethodDetails postPersistMethod;

	private final MethodDetails preRemoveMethod;
	private final MethodDetails postRemoveMethod;

	private final MethodDetails preUpdateMethod;
	private final MethodDetails postUpdateMethod;

	private final MethodDetails postLoadMethod;

	/// Create a lifecycle callback descriptor from already-resolved callback methods.
	public JpaEventListener(
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

	/// The style that determines the expected callback method signature.
	public JpaEventListenerStyle getStyle() {
		return consumerType;
	}

	/// The managed type or listener class declaring the callback methods.
	public ClassDetails getCallbackClass() {
		return listenerClass;
	}

	/// Callback method for {@link PrePersist}, or {@code null} when none is declared.
	public MethodDetails getPrePersistMethod() {
		return prePersistMethod;
	}

	/// Callback method for {@link PostPersist}, or {@code null} when none is declared.
	public MethodDetails getPostPersistMethod() {
		return postPersistMethod;
	}

	/// Callback method for {@link PreRemove}, or {@code null} when none is declared.
	public MethodDetails getPreRemoveMethod() {
		return preRemoveMethod;
	}

	/// Callback method for {@link PostRemove}, or {@code null} when none is declared.
	public MethodDetails getPostRemoveMethod() {
		return postRemoveMethod;
	}

	/// Callback method for {@link PreUpdate}, or {@code null} when none is declared.
	public MethodDetails getPreUpdateMethod() {
		return preUpdateMethod;
	}

	/// Callback method for {@link PostUpdate}, or {@code null} when none is declared.
	public MethodDetails getPostUpdateMethod() {
		return postUpdateMethod;
	}

	/// Callback method for {@link PostLoad}, or {@code null} when none is declared.
	public MethodDetails getPostLoadMethod() {
		return postLoadMethod;
	}

	/**
	 * Create a listener descriptor from XML (with explicitly named methods)
	 *
	 * @see JaxbPersistenceUnitDefaultsImpl#getEntityListenerContainer()
	 * @see JaxbEntityListenerImpl
	 * @see GlobalRegistrations#getEntityListenerRegistrations()
	 */
	public static JpaEventListener from(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClassDetails,
			JaxbEntityListenerImpl jaxbMapping) {
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			if ( jaxbMapping.getPrePersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostPersist().getMethodName() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostPersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postPersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreRemove().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preRemoveMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostRemove().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postRemoveMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpdate().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpdateMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpdate().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpdateMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostLoad() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostLoad().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postLoadMethod.set( methodDetails );
			}
		} );

		final JpaEventListener jpaEventListener = new JpaEventListener(
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

		errorIfEmpty( jpaEventListener );

		return jpaEventListener;
	}

	private static void errorIfEmpty(JpaEventListener jpaEventListener) {
		if ( jpaEventListener.prePersistMethod == null
				&& jpaEventListener.postPersistMethod == null
				&& jpaEventListener.preRemoveMethod == null
				&& jpaEventListener.postRemoveMethod == null
				&& jpaEventListener.preUpdateMethod == null
				&& jpaEventListener.postUpdateMethod == null
				&& jpaEventListener.postLoadMethod == null ) {
			throw new ModelsException( "Mapping for entity-listener specified no callback methods - " + jpaEventListener.listenerClass.getClassName() );
		}
	}

	/// Create a listener descriptor from annotation-declared callback methods.
	public static JpaEventListener from(JpaEventListenerStyle consumerType, ClassDetails listenerClassDetails) {
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			if ( methodDetails.hasDirectAnnotationUsage( PrePersist.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostPersist.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postPersistMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreRemove.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostRemove.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreUpdate.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostUpdate.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostLoad.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postLoadMethod.set( methodDetails );
			}
		} );

		final JpaEventListener jpaEventListener = new JpaEventListener(
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
		errorIfEmpty( jpaEventListener );
		return jpaEventListener;
	}

	/// Try to interpret the class as an entity callback class.
	///
	/// @return the callback descriptor, or {@code null} when no callback methods are declared
	public static JpaEventListener tryAsCallback(ClassDetails classDetails) {
		try {
			return from( JpaEventListenerStyle.CALLBACK, classDetails );
		}
		catch ( ModelsException e ) {
			return null;
		}
	}

	/// Whether the method has the legal signature for the given callback style.
	public static boolean matchesSignature(JpaEventListenerStyle callbackType, MethodDetails methodDetails) {
		if ( callbackType == JpaEventListenerStyle.CALLBACK ) {
			// should have no arguments.  and technically (spec) have a void return
			return methodDetails.getArgumentTypes().isEmpty()
					&& methodDetails.getReturnType() == ClassDetails.VOID_CLASS_DETAILS;
		}
		else {
			assert callbackType == JpaEventListenerStyle.LISTENER;
			// should have 1 argument.  and technically (spec) have a void return
			return methodDetails.getArgumentTypes().size() == 1
					&& methodDetails.getReturnType() == ClassDetails.VOID_CLASS_DETAILS;
		}
	}

}
