/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

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

	private final MethodDetails preInsertMethod;
	private final MethodDetails postInsertMethod;

	private final MethodDetails preRemoveMethod;
	private final MethodDetails postRemoveMethod;

	private final MethodDetails preDeleteMethod;
	private final MethodDetails postDeleteMethod;

	private final MethodDetails preMergeMethod;

	private final MethodDetails preUpdateMethod;
	private final MethodDetails postUpdateMethod;

	private final MethodDetails preUpsertMethod;
	private final MethodDetails postUpsertMethod;

	private final MethodDetails postLoadMethod;

	/// Create a lifecycle callback descriptor from already-resolved callback methods.
	public JpaEventListener(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClass,
			MethodDetails prePersistMethod,
			MethodDetails postPersistMethod,
			MethodDetails preInsertMethod,
			MethodDetails postInsertMethod,
			MethodDetails preRemoveMethod,
			MethodDetails postRemoveMethod,
			MethodDetails preDeleteMethod,
			MethodDetails postDeleteMethod,
			MethodDetails preMergeMethod,
			MethodDetails preUpdateMethod,
			MethodDetails postUpdateMethod,
			MethodDetails preUpsertMethod,
			MethodDetails postUpsertMethod,
			MethodDetails postLoadMethod) {
		this.consumerType = consumerType;
		this.listenerClass = listenerClass;
		this.prePersistMethod = prePersistMethod;
		this.postPersistMethod = postPersistMethod;
		this.preInsertMethod = preInsertMethod;
		this.postInsertMethod = postInsertMethod;
		this.preRemoveMethod = preRemoveMethod;
		this.postRemoveMethod = postRemoveMethod;
		this.preDeleteMethod = preDeleteMethod;
		this.postDeleteMethod = postDeleteMethod;
		this.preMergeMethod = preMergeMethod;
		this.preUpdateMethod = preUpdateMethod;
		this.postUpdateMethod = postUpdateMethod;
		this.preUpsertMethod = preUpsertMethod;
		this.postUpsertMethod = postUpsertMethod;
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

	/// Callback method for {@link PreInsert}, or {@code null} when none is declared.
	public MethodDetails getPreInsertMethod() {
		return preInsertMethod;
	}

	/// Callback method for {@link PostInsert}, or {@code null} when none is declared.
	public MethodDetails getPostInsertMethod() {
		return postInsertMethod;
	}

	/// Callback method for {@link PreRemove}, or {@code null} when none is declared.
	public MethodDetails getPreRemoveMethod() {
		return preRemoveMethod;
	}

	/// Callback method for {@link PostRemove}, or {@code null} when none is declared.
	public MethodDetails getPostRemoveMethod() {
		return postRemoveMethod;
	}

	/// Callback method for {@link PreDelete}, or {@code null} when none is declared.
	public MethodDetails getPreDeleteMethod() {
		return preDeleteMethod;
	}

	/// Callback method for {@link PostDelete}, or {@code null} when none is declared.
	public MethodDetails getPostDeleteMethod() {
		return postDeleteMethod;
	}

	/// Callback method for {@link PreMerge}, or {@code null} when none is declared.
	public MethodDetails getPreMergeMethod() {
		return preMergeMethod;
	}

	/// Callback method for {@link PreUpdate}, or {@code null} when none is declared.
	public MethodDetails getPreUpdateMethod() {
		return preUpdateMethod;
	}

	/// Callback method for {@link PostUpdate}, or {@code null} when none is declared.
	public MethodDetails getPostUpdateMethod() {
		return postUpdateMethod;
	}

	/// Callback method for {@link PreUpsert}, or {@code null} when none is declared.
	public MethodDetails getPreUpsertMethod() {
		return preUpsertMethod;
	}

	/// Callback method for {@link PostUpsert}, or {@code null} when none is declared.
	public MethodDetails getPostUpsertMethod() {
		return postUpsertMethod;
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
		if ( isImplicitMethodMappings( jaxbMapping ) ) {
			return from( consumerType, listenerClassDetails );
		}

		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preMergeMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			if ( jaxbMapping.getPrePersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostPersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostPersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postPersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreInsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreInsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preInsertMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostInsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostInsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postInsertMethod.set( methodDetails );
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
			else if ( jaxbMapping.getPreDelete() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreDelete().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preDeleteMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostDelete() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostDelete().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postDeleteMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreMerge() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreMerge().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preMergeMethod.set( methodDetails );
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
			else if ( jaxbMapping.getPreUpsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpsertMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostUpsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpsertMethod.set( methodDetails );
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
				preInsertMethod.get(),
				postInsertMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preDeleteMethod.get(),
				postDeleteMethod.get(),
				preMergeMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				preUpsertMethod.get(),
				postUpsertMethod.get(),
				postLoadMethod.get()
		);

		errorIfEmpty( jpaEventListener );

		return jpaEventListener;
	}

	private static boolean isImplicitMethodMappings(JaxbEntityListenerImpl jaxbMapping) {
		return jaxbMapping.getPrePersist() == null
			&& jaxbMapping.getPostPersist() == null
			&& jaxbMapping.getPreInsert() == null
			&& jaxbMapping.getPostInsert() == null
			&& jaxbMapping.getPreRemove() == null
			&& jaxbMapping.getPostRemove() == null
			&& jaxbMapping.getPreDelete() == null
			&& jaxbMapping.getPostDelete() == null
			&& jaxbMapping.getPreMerge() == null
			&& jaxbMapping.getPreUpdate() == null
			&& jaxbMapping.getPostUpdate() == null
			&& jaxbMapping.getPreUpsert() == null
			&& jaxbMapping.getPostUpsert() == null
			&& jaxbMapping.getPostLoad() == null;
	}

	private static void errorIfEmpty(JpaEventListener jpaEventListener) {
		if ( jpaEventListener.prePersistMethod == null
				&& jpaEventListener.postPersistMethod == null
				&& jpaEventListener.preInsertMethod == null
				&& jpaEventListener.postInsertMethod == null
				&& jpaEventListener.preRemoveMethod == null
				&& jpaEventListener.postRemoveMethod == null
				&& jpaEventListener.preDeleteMethod == null
				&& jpaEventListener.postDeleteMethod == null
				&& jpaEventListener.preMergeMethod == null
				&& jpaEventListener.preUpdateMethod == null
				&& jpaEventListener.postUpdateMethod == null
				&& jpaEventListener.preUpsertMethod == null
				&& jpaEventListener.postUpsertMethod == null
				&& jpaEventListener.postLoadMethod == null ) {
			throw new ModelsException( "Mapping for entity-listener specified no callback methods - " + jpaEventListener.listenerClass.getClassName() );
		}
	}

	/// Create a listener descriptor from annotation-declared callback methods.
	public static JpaEventListener from(JpaEventListenerStyle consumerType, ClassDetails listenerClassDetails) {
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preMergeMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpsertMethod = new MutableObject<>();
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
			else if ( methodDetails.hasDirectAnnotationUsage( PreInsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preInsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostInsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postInsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreRemove.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostRemove.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postRemoveMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreDelete.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preDeleteMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostDelete.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postDeleteMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreMerge.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preMergeMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreUpdate.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostUpdate.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpdateMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreUpsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostUpsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpsertMethod.set( methodDetails );
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
				preInsertMethod.get(),
				postInsertMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preDeleteMethod.get(),
				postDeleteMethod.get(),
				preMergeMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				preUpsertMethod.get(),
				postUpsertMethod.get(),
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
