/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PreUpsert;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import java.lang.annotation.Annotation;
import java.util.Locale;

import static org.hibernate.boot.mapping.internal.categorize.JpaEventListener.matchesSignature;

/// AllMemberConsumer impl for collecting callback methods
///
/// @since 9.0
/// @author Steve Ebersole
public class LifecycleCallbackCollector implements AllMemberConsumer {
	private final ClassDetails managedTypeDetails;

	private MethodDetails prePersist;
	private MethodDetails postPersist;
	private MethodDetails preInsert;
	private MethodDetails postInsert;
	private MethodDetails preUpdate;
	private MethodDetails postUpdate;
	private MethodDetails preUpsert;
	private MethodDetails postUpsert;
	private MethodDetails preRemove;
	private MethodDetails postRemove;
	private MethodDetails preDelete;
	private MethodDetails postDelete;
	private MethodDetails preMerge;
	private MethodDetails postLoad;

	public LifecycleCallbackCollector(ClassDetails managedTypeDetails) {
		this.managedTypeDetails = managedTypeDetails;
	}

	@Override
	public void acceptMember(MemberDetails memberDetails) {
		if ( memberDetails.isField() ) {
			return;
		}

		final MethodDetails methodDetails = (MethodDetails) memberDetails;

		if ( methodDetails.hasDirectAnnotationUsage( PrePersist.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			prePersist = apply( methodDetails, PrePersist.class, managedTypeDetails, prePersist );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostPersist.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postPersist = apply( methodDetails, PostPersist.class, managedTypeDetails, postPersist );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreInsert.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preInsert = apply( methodDetails, PreInsert.class, managedTypeDetails, preInsert );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostInsert.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postInsert = apply( methodDetails, PostInsert.class, managedTypeDetails, postInsert );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreRemove.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preRemove = apply( methodDetails, PreRemove.class, managedTypeDetails, preRemove );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostRemove.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postRemove = apply( methodDetails, PostRemove.class, managedTypeDetails, postRemove );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreDelete.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preDelete = apply( methodDetails, PreDelete.class, managedTypeDetails, preDelete );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostDelete.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postDelete = apply( methodDetails, PostDelete.class, managedTypeDetails, postDelete );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreMerge.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preMerge = apply( methodDetails, PreMerge.class, managedTypeDetails, preMerge );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreUpdate.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preUpdate = apply( methodDetails, PreUpdate.class, managedTypeDetails, preUpdate );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostUpdate.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postUpdate = apply( methodDetails, PostUpdate.class, managedTypeDetails, postUpdate );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PreUpsert.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preUpsert = apply( methodDetails, PreUpsert.class, managedTypeDetails, preUpsert );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostUpsert.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postUpsert = apply( methodDetails, PostUpsert.class, managedTypeDetails, postUpsert );
		}
		else if ( methodDetails.hasDirectAnnotationUsage( PostLoad.class )
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postLoad = apply( methodDetails, PostLoad.class, managedTypeDetails, postLoad );
		}
	}

	private static <A extends Annotation> MethodDetails apply(
			MethodDetails incomingValue,
			Class<A> annotationType,
			ClassDetails managedTypeDetails,
			MethodDetails currentValue) {
		if ( currentValue != null ) {
			throw new ModelsException(
					String.format(
							Locale.ROOT,
							"Encountered multiple @%s methods [%s] - %s, %s",
							annotationType.getSimpleName(),
							managedTypeDetails.getClassName(),
							currentValue.getName(),
							incomingValue.getName()
					)
			);
		}
		return incomingValue;
	}

	public JpaEventListener resolve() {
		if ( prePersist != null
				|| postPersist != null
				|| preInsert != null
				|| postInsert != null
				|| preUpdate != null
				|| postUpdate != null
				|| preUpsert != null
				|| postUpsert != null
				|| preRemove != null
				|| postRemove != null
				|| preDelete != null
				|| postDelete != null
				|| preMerge != null
				|| postLoad != null ) {
			return new JpaEventListener(
					JpaEventListenerStyle.CALLBACK,
					managedTypeDetails,
					prePersist,
					postPersist,
					preInsert,
					postInsert,
					preRemove,
					postRemove,
					preDelete,
					postDelete,
					preMerge,
					preUpdate,
					postUpdate,
					preUpsert,
					postUpsert,
					postLoad
			);
		}
		return null;
	}
}
