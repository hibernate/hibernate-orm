/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.models.JpaEventListenerStyle;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import static org.hibernate.boot.models.JpaAnnotations.POST_LOAD;
import static org.hibernate.boot.models.JpaAnnotations.POST_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.POST_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.POST_UPDATE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.PRE_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_UPDATE;

/**
 * JPA-style event listener with support for resolving callback methods from
 * {@linkplain #from(JpaEventListenerStyle, ClassDetails, JaxbEntityListenerImpl, ModelsContext) XML}
 * or from {@linkplain #from(JpaEventListenerStyle, ClassDetails) annotation}.
 * <p/>
 * Represents a global entity listener defined in the persistence unit
 *
 * @see JaxbPersistenceUnitDefaultsImpl#getEntityListenerContainer()
 * @see JaxbEntityListenerImpl
 * @see GlobalRegistrations#getEntityListenerRegistrations()
 *
 * @see jakarta.persistence.PostLoad
 * @see jakarta.persistence.PostPersist
 * @see jakarta.persistence.PostRemove
 * @see jakarta.persistence.PostUpdate
 * @see jakarta.persistence.PrePersist
 * @see jakarta.persistence.PreRemove
 * @see jakarta.persistence.PreUpdate
 *
 * @author Steve Ebersole
 */
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

	/**
	 * Create a listener descriptor from XML
	 *
	 * @see JaxbPersistenceUnitDefaultsImpl#getEntityListenerContainer()
	 * @see JaxbEntityListenerImpl
	 * @see GlobalRegistrations#getEntityListenerRegistrations()
	 */
	public static JpaEventListener from(
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
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_PERSIST.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostPersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostPersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postPersistMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_PERSIST.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPreRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreRemove().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preRemoveMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_REMOVE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostRemove().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postRemoveMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_REMOVE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPreUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpdate().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpdateMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_UPDATE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpdate().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpdateMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_UPDATE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostLoad() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostLoad().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postLoadMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_LOAD.createUsage( modelsContext ) );
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

	private static boolean isImplicitMethodMappings(JaxbEntityListenerImpl jaxbMapping) {
		return jaxbMapping.getPrePersist() == null
			&& jaxbMapping.getPreUpdate() == null
			&& jaxbMapping.getPreRemove() == null
			&& jaxbMapping.getPostLoad() == null
			&& jaxbMapping.getPostPersist() == null
			&& jaxbMapping.getPostUpdate() == null
			&& jaxbMapping.getPostRemove() == null;
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

	/**
	 * Create a listener descriptor from annotations
	 *
	 * @see jakarta.persistence.EntityListeners
	 * @see GlobalRegistrations#getEntityListenerRegistrations()
	 */
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
