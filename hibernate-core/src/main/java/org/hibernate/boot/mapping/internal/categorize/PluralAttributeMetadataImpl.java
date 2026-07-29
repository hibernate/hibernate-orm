/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.annotation.Nullable;

import org.hibernate.boot.models.AttributeNature;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

/// Standard [PluralAttributeMetadata] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public record PluralAttributeMetadataImpl(
		String name,
		AttributeNature nature,
		MemberDetails member,
		TypeDetails collectionType,
		CollectionClassification collectionClassification,
		ValueMetadata element,
		@Nullable ValueMetadata index,
		@Nullable CollectionIdMetadata collectionId) implements PluralAttributeMetadata {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public AttributeNature getNature() {
		return nature;
	}

	@Override
	public MemberDetails getMember() {
		return member;
	}

	@Override
	public TypeDetails getCollectionType() {
		return collectionType;
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	@Override
	public ValueMetadata getElement() {
		return element;
	}

	@Override
	public ValueMetadata getIndex() {
		return index;
	}

	@Override
	public CollectionIdMetadata getCollectionId() {
		return collectionId;
	}
}
