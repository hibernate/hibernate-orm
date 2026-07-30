/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.categorize.AttributeMetadataImplementor;
import org.hibernate.boot.mapping.internal.categorize.AbstractIdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;

/// Local state for an inverse plural association whose physical table/key details
/// are resolved from the owning side after table keys have been bound.
///
/// Inverse plural attributes are encountered while binding members, but the
/// mapping value is mostly a projection of the owning side.  The inverse phase
/// uses [#mappedBy()] to find the owning property and copies the relevant table,
/// key, element, and map-key structure once those owning-side structures are
/// stable.
///
/// @since 9.0
/// @author Steve Ebersole
public record InversePluralAssociationBinding(
		Nature nature,
		AbstractIdentifiableTypeMetadata ownerType,
		PersistentClass ownerBinding,
		AttributeMetadataImplementor attributeMetadata,
		CollectionSource source,
		Collection collection,
		ClassDetails targetClassDetails,
		String mappedBy) {
	public enum Nature {
		MANY_TO_MANY,
		ONE_TO_MANY
	}
}
