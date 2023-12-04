/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.jdk.VoidClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * JPA-style event listener with support for resolving callback methods
 * {@linkplain #from(JpaEventListenerStyle, ClassDetails, JaxbEntityListenerImpl) by name} (from XML)
 * or by {@linkplain #from(JpaEventListenerStyle, ClassDetails) annotation}.
 *
 * Represents a global entity listener defined in the persistence unit
 *
 * @see JaxbPersistenceUnitDefaultsImpl#getEntityListeners()
 * @see JaxbEntityListenerImpl
 * @see GlobalRegistrations#getEntityListenerRegistrations()
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
	 * Create a listener descriptor from XML (with explicitly named methods)
	 *
	 * @see JaxbPersistenceUnitDefaultsImpl#getEntityListeners()
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

	public static JpaEventListener from(JpaEventListenerStyle consumerType, ClassDetails listenerClassDetails) {
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			if ( methodDetails.getAnnotationUsage( PrePersist.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PostPersist.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				postPersistMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PreRemove.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				preRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PostRemove.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				postRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PreUpdate.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PostUpdate.class ) != null
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.getAnnotationUsage( PostLoad.class ) != null
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

	public static JpaEventListener tryAsCallback(ClassDetails classDetails) {
		try {
			return from( JpaEventListenerStyle.CALLBACK, classDetails );
		}
		catch ( ModelsException e ) {
			return null;
		}
	}

	public static boolean matchesSignature(JpaEventListenerStyle callbackType, MethodDetails methodDetails) {
		if ( callbackType == JpaEventListenerStyle.CALLBACK ) {
			// should have no arguments.  and technically (spec) have a void return
			return methodDetails.getArgumentTypes().isEmpty()
					&& methodDetails.getReturnType() == VoidClassDetails.VOID_CLASS_DETAILS;
		}
		else {
			assert callbackType == JpaEventListenerStyle.LISTENER;
			// should have 1 argument.  and technically (spec) have a void return
			return methodDetails.getArgumentTypes().size() == 1
					&& methodDetails.getReturnType() == VoidClassDetails.VOID_CLASS_DETAILS;
		}
	}

}
