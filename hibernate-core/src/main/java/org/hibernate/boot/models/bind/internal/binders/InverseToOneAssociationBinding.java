/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;

/// Local state for an inverse to-one association resolved from its owning side.
///
/// A mapped-by one-to-one does not own columns.  It is bound first as a logical
/// inverse value and completed later from the owning association, after member
/// binding and table-key binding have made direct-column and join-table owning
/// forms visible.
///
/// @since 9.0
/// @author Steve Ebersole
public record InverseToOneAssociationBinding(
		IdentifiableTypeMetadata ownerType,
		PersistentClass ownerBinding,
		AttributeMetadata attributeMetadata,
		Property property,
		OneToOne value,
		ClassDetails targetClassDetails,
		String mappedBy) {
}
