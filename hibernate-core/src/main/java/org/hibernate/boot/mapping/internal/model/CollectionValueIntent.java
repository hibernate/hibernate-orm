/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

/// Source-level intent for a collection-valued attribute usage.
///
/// A collection usage is an aggregate mapping request.  The plural attribute
/// contributes the collection container itself, an owner key/table relationship,
/// an element value, and sometimes an index/map-key value or id-bag identifier.
/// This intent records those nested value requests without retaining the
/// compatibility `org.hibernate.mapping.Collection`, key, element, index, or
/// table objects produced by materialization.
///
/// Owner-key, join-table, and foreign-key details remain support facts on the
/// [CollectionSource].  They are deliberately not modeled as [ValueIntent]
/// instances because they describe how the collection table relates to another
/// table, not a user-visible collection part.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollectionValueIntent(
		CollectionSource source,
		CollectionSource.Nature collectionNature,
		CollectionClassification classification,
		String sourceRole,
		String attributePath,
		ValueIntent elementIntent,
		@Nullable ValueIntent indexIntent,
		@Nullable BasicValueIntent collectionIdIntent) implements ValueIntent {
	@Override
	public AttributeNature nature() {
		return switch ( collectionNature ) {
			case ELEMENT_COLLECTION -> AttributeNature.ELEMENT_COLLECTION;
			case MANY_TO_MANY -> AttributeNature.MANY_TO_MANY;
			case ONE_TO_MANY -> AttributeNature.ONE_TO_MANY;
			case MANY_TO_ANY -> AttributeNature.MANY_TO_ANY;
		};
	}

	public static CollectionValueIntent fromAttribute(
			CollectionSource source,
			String sourceRole,
			String attributePath,
			BindingState bindingState,
			BindingContext bindingContext) {
		return fromUsage( source, sourceRole, attributePath, bindingState, bindingContext );
	}

	public static CollectionValueIntent fromUsage(
			CollectionSource source,
			String sourceRole,
			String attributePath,
			BindingState bindingState,
			BindingContext bindingContext) {
		return new CollectionValueIntent(
				source,
				source.nature(),
				source.classification(),
				sourceRole,
				attributePath,
				elementIntent( source, sourceRole, bindingState, bindingContext ),
				indexIntent( source, sourceRole ),
				collectionIdIntent( source )
		);
	}

	private static ValueIntent elementIntent(
			CollectionSource source,
			String sourceRole,
			BindingState bindingState,
			BindingContext bindingContext) {
		return switch ( source.nature() ) {
			case ELEMENT_COLLECTION -> source.hasEmbeddableElement()
					? new EmbeddedValueIntent( source.elementType(), source.member().resolveAttributeName(), sourceRole + ".<element>" )
					: BasicValueIntent.fromCollectionElement( source );
			case MANY_TO_MANY, ONE_TO_MANY -> new ToOneValueIntent(
					source.elementType(),
					source.member().resolveAttributeName(),
					sourceRole + ".<element>",
					null
			);
			case MANY_TO_ANY -> new AnyValueIntent(
					AnySource.createManyToAny( source, bindingContext, bindingState )
			);
		};
	}

	private static @Nullable ValueIntent indexIntent(CollectionSource source, String sourceRole) {
		if ( source.classification() == CollectionClassification.LIST
				|| source.classification() == CollectionClassification.ARRAY ) {
			return BasicValueIntent.fromListIndex( source );
		}
		if ( source.mapKeyType() == null ) {
			return null;
		}
		if ( source.mapKey() != null ) {
			return null;
		}

		final ClassDetails mapKeyType = source.mapKeyType().determineRawClass();
		if ( !source.mapKeyJoinColumns().isEmpty() || mapKeyType.hasDirectAnnotationUsage( Entity.class ) ) {
			return new ToOneValueIntent(
					source.mapKeyType(),
					source.member().resolveAttributeName() + ".<map-key>",
					sourceRole + ".<map-key>",
					null
			);
		}
		if ( mapKeyType.hasDirectAnnotationUsage( Embeddable.class ) ) {
			return new EmbeddedValueIntent(
					source.mapKeyType(),
					source.member().resolveAttributeName() + ".<map-key>",
					sourceRole + ".<map-key>"
			);
		}
		return BasicValueIntent.fromMapKey( source );
	}

	private static @Nullable BasicValueIntent collectionIdIntent(CollectionSource source) {
		if ( source.classification() != CollectionClassification.ID_BAG
				&& !source.member().hasDirectAnnotationUsage( CollectionId.class ) ) {
			return null;
		}
		return BasicValueIntent.fromCollectionId( source );
	}
}
