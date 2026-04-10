/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PreUpsert;
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

import static org.hibernate.boot.models.JpaAnnotations.POST_DELETE;
import static org.hibernate.boot.models.JpaAnnotations.POST_INSERT;
import static org.hibernate.boot.models.JpaAnnotations.POST_LOAD;
import static org.hibernate.boot.models.JpaAnnotations.POST_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.POST_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.POST_UPDATE;
import static org.hibernate.boot.models.JpaAnnotations.POST_UPSERT;
import static org.hibernate.boot.models.JpaAnnotations.PRE_DELETE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_INSERT;
import static org.hibernate.boot.models.JpaAnnotations.PRE_MERGE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_PERSIST;
import static org.hibernate.boot.models.JpaAnnotations.PRE_REMOVE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_UPDATE;
import static org.hibernate.boot.models.JpaAnnotations.PRE_UPSERT;

/**
 * JPA-style event listener with support for resolving callback methods from
 * {@linkplain #from(JpaEventListenerStyle, ClassDetails, JaxbEntityListenerImpl, ModelsContext) XML}
 * or from {@linkplain #from(JpaEventListenerStyle, ClassDetails) annotation}.
 * <p>
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
 * @see jakarta.persistence.PostUpsert
 * @see jakarta.persistence.PostInsert
 * @see jakarta.persistence.PostDelete
 * @see jakarta.persistence.PreMerge
 * @see jakarta.persistence.PrePersist
 * @see jakarta.persistence.PreRemove
 * @see jakarta.persistence.PreUpdate
 * @see jakarta.persistence.PreUpsert
 * @see jakarta.persistence.PreInsert
 * @see jakarta.persistence.PreDelete
 *
 * @author Steve Ebersole
 */
public class JpaEventListener {

	private final JpaEventListenerStyle consumerType;
	private final ClassDetails listenerClass;

	private final MethodDetails preMergeMethod;

	private final MethodDetails prePersistMethod;
	private final MethodDetails postPersistMethod;

	private final MethodDetails preRemoveMethod;
	private final MethodDetails postRemoveMethod;

	private final MethodDetails preUpdateMethod;
	private final MethodDetails postUpdateMethod;

	private final MethodDetails preUpsertMethod;
	private final MethodDetails postUpsertMethod;

	private final MethodDetails preInsertMethod;
	private final MethodDetails postInsertMethod;

	private final MethodDetails preDeleteMethod;
	private final MethodDetails postDeleteMethod;

	private final MethodDetails postLoadMethod;

	public JpaEventListener(
			JpaEventListenerStyle consumerType,
			ClassDetails listenerClass,
			MethodDetails preMergeMethod,
			MethodDetails prePersistMethod,
			MethodDetails postPersistMethod,
			MethodDetails preRemoveMethod,
			MethodDetails postRemoveMethod,
			MethodDetails preUpdateMethod,
			MethodDetails postUpdateMethod,
			MethodDetails preUpsertMethod,
			MethodDetails postUpsertMethod,
			MethodDetails preInsertMethod,
			MethodDetails postInsertMethod,
			MethodDetails preDeleteMethod,
			MethodDetails postDeleteMethod,
			MethodDetails postLoadMethod) {
		this.consumerType = consumerType;
		this.listenerClass = listenerClass;
		this.preMergeMethod = preMergeMethod;
		this.prePersistMethod = prePersistMethod;
		this.postPersistMethod = postPersistMethod;
		this.preRemoveMethod = preRemoveMethod;
		this.postRemoveMethod = postRemoveMethod;
		this.preUpdateMethod = preUpdateMethod;
		this.postUpdateMethod = postUpdateMethod;
		this.preUpsertMethod = preUpsertMethod;
		this.postUpsertMethod = postUpsertMethod;
		this.preInsertMethod = preInsertMethod;
		this.postInsertMethod = postInsertMethod;
		this.preDeleteMethod = preDeleteMethod;
		this.postDeleteMethod = postDeleteMethod;
		this.postLoadMethod = postLoadMethod;
	}

	public JpaEventListenerStyle getStyle() {
		return consumerType;
	}

	public ClassDetails getCallbackClass() {
		return listenerClass;
	}

	public MethodDetails getPreMergeMethod() {
		return preMergeMethod;
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

	public MethodDetails getPreUpsertMethod() {
		return preUpsertMethod;
	}

	public MethodDetails getPostUpsertMethod() {
		return postUpsertMethod;
	}

	public MethodDetails getPreInsertMethod() {
		return preInsertMethod;
	}

	public MethodDetails getPostInsertMethod() {
		return postInsertMethod;
	}

	public MethodDetails getPreDeleteMethod() {
		return preDeleteMethod;
	}

	public MethodDetails getPostDeleteMethod() {
		return postDeleteMethod;
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
		final MutableObject<MethodDetails> preMergeMethod = new MutableObject<>();
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		if ( isImplicitMethodMappings( jaxbMapping ) ) {
			return from( consumerType, listenerClassDetails );
		}

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			final MutableMemberDetails mutableMethodDetails = (MutableMemberDetails) methodDetails;
			if ( jaxbMapping.getPreMerge() != null
				&& methodDetails.getName().equals( jaxbMapping.getPreMerge().getMethodName() )
				&& matchesSignature( consumerType, methodDetails ) ) {
				preMergeMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_MERGE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPrePersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				prePersistMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_PERSIST.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPreDelete() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreDelete().getMethodName() )
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
			else if ( jaxbMapping.getPreUpsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpsertMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_UPSERT.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostUpsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpsertMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_UPSERT.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPreInsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreInsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preInsertMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_INSERT.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostInsert() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostInsert().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postInsertMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_INSERT.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPreDelete() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreDelete().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preDeleteMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( PRE_DELETE.createUsage( modelsContext ) );
			}
			else if ( jaxbMapping.getPostDelete() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostDelete().getMethodName() )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postDeleteMethod.set( methodDetails );
				mutableMethodDetails.addAnnotationUsage( POST_DELETE.createUsage( modelsContext ) );
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
				preMergeMethod.get(),
				prePersistMethod.get(),
				postPersistMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				postLoadMethod.get(),
				preUpsertMethod.get(),
				postUpsertMethod.get(),
				preInsertMethod.get(),
				postInsertMethod.get(),
				preDeleteMethod.get(),
				postDeleteMethod.get()
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
		final MutableObject<MethodDetails> preMergeMethod = new MutableObject<>();
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postInsertMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postDeleteMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			if ( methodDetails.hasDirectAnnotationUsage( PreMerge.class )
				&& matchesSignature( consumerType, methodDetails ) ) {
				preMergeMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PrePersist.class )
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
			else if ( methodDetails.hasDirectAnnotationUsage( PreUpsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preUpsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostUpsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postUpsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreInsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preInsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostInsert.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postInsertMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PreDelete.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				preDeleteMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostDelete.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postDeleteMethod.set( methodDetails );
			}
			else if ( methodDetails.hasDirectAnnotationUsage( PostLoad.class )
					&& matchesSignature( consumerType, methodDetails ) ) {
				postLoadMethod.set( methodDetails );
			}
		} );

		final JpaEventListener jpaEventListener = new JpaEventListener(
				consumerType,
				listenerClassDetails,
				preMergeMethod.get(),
				prePersistMethod.get(),
				postPersistMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				postLoadMethod.get(),
				preUpsertMethod.get(),
				postUpsertMethod.get(),
				preInsertMethod.get(),
				postInsertMethod.get(),
				preDeleteMethod.get(),
				postDeleteMethod.get()
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
