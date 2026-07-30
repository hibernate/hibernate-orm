/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.TypeDetails;

/// Categorized plural attribute and its element, index, and identifier values.
///
/// Collection-valued attributes always have an element value. Indexed
/// collections additionally have an index value, and id-bags additionally
/// expose collection-id metadata.
///
/// @since 9.0
/// @author Steve Ebersole
public interface PluralAttributeMetadata extends AttributeMetadata {
	/// The declared Java collection type.
	TypeDetails getCollectionType();

	@Override
	default TypeDetails getAttributeType() {
		return getCollectionType();
	}

	/// The Hibernate collection classification.
	CollectionClassification getCollectionClassification();

	/// The collection element value.
	ValueMetadata getElement();

	/// The collection index or map-key value, or `null` for a non-indexed
	/// collection.
	@Nullable
	ValueMetadata getIndex();

	/// The id-bag identifier metadata, or `null` when the collection has no
	/// collection identifier.
	@Nullable
	CollectionIdMetadata getCollectionId();
}
