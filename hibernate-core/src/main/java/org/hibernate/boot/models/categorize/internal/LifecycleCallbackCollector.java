/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.Locale;

import org.hibernate.boot.models.categorize.spi.AllMemberConsumer;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import static org.hibernate.boot.models.categorize.spi.JpaEventListener.matchesSignature;

/**
 * @author Steve Ebersole
 */
public class LifecycleCallbackCollector implements AllMemberConsumer {
	private final ClassDetails managedTypeDetails;
	private final ModelCategorizationContext modelContext;

	private MethodDetails prePersist;
	private MethodDetails postPersist;
	private MethodDetails preUpdate;
	private MethodDetails postUpdate;
	private MethodDetails preRemove;
	private MethodDetails postRemove;
	private MethodDetails postLoad;

	public LifecycleCallbackCollector(ClassDetails managedTypeDetails, ModelCategorizationContext modelContext) {
		this.managedTypeDetails = managedTypeDetails;
		this.modelContext = modelContext;
	}

	@Override
	public void acceptMember(MemberDetails memberDetails) {
		if ( memberDetails.isField() ) {
			return;
		}

		final MethodDetails methodDetails = (MethodDetails) memberDetails;

		if ( methodDetails.getAnnotationUsage( PrePersist.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			prePersist = apply( methodDetails, PrePersist.class, managedTypeDetails, prePersist );
		}
		else if ( methodDetails.getAnnotationUsage( PostPersist.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postPersist = apply( methodDetails, PostPersist.class, managedTypeDetails, postPersist );
		}
		else if ( methodDetails.getAnnotationUsage( PreRemove.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preRemove = apply( methodDetails, PreRemove.class, managedTypeDetails, preRemove );
		}
		else if ( methodDetails.getAnnotationUsage( PostRemove.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postRemove = apply( methodDetails, PostRemove.class, managedTypeDetails, postRemove );
		}
		else if ( methodDetails.getAnnotationUsage( PreUpdate.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			preUpdate = apply( methodDetails, PreUpdate.class, managedTypeDetails, preUpdate );
		}
		else if ( methodDetails.getAnnotationUsage( PostUpdate.class ) != null
				&& matchesSignature( JpaEventListenerStyle.CALLBACK, methodDetails ) ) {
			postUpdate = apply( methodDetails, PostUpdate.class, managedTypeDetails, postUpdate );
		}
		else if ( methodDetails.getAnnotationUsage( PostLoad.class ) != null
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
				|| preUpdate != null
				|| postUpdate != null
				|| preRemove != null
				|| postRemove != null
				|| postLoad != null ) {
			return new JpaEventListener(
					JpaEventListenerStyle.CALLBACK,
					managedTypeDetails,
					prePersist,
					postPersist,
					preRemove,
					postRemove,
					preUpdate,
					postUpdate,
					postLoad
			);
		}
		return null;
	}
}
