/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.TypeDetails;

/// Categorized metadata for a plural attribute and its nested value parts.
///
/// @since 9.0
/// @author Steve Ebersole
public interface PluralAttributeMetadata extends AttributeMetadata {
	/// The resolved collection container type at this usage site.
	TypeDetails getCollectionType();

	@Override
	default TypeDetails getAttributeType() {
		return getCollectionType();
	}

	CollectionClassification getCollectionClassification();

	ValueMetadata getElement();

	/// The list/array index or map key, when the collection has one.
	@Nullable
	ValueMetadata getIndex();

	/// The id-bag identifier, when present.
	@Nullable
	CollectionIdMetadata getCollectionId();
}
