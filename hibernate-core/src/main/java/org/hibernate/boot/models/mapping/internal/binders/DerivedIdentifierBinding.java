/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import java.util.List;

import org.hibernate.boot.models.mapping.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import jakarta.persistence.JoinColumn;

/// Pending binding for a derived identifier association such as `@MapsId`.
///
/// Member binding creates the owning to-one property, but `@MapsId` means its
/// columns are not new association columns.  They are the already-bound columns
/// of the owner's identifier, or one named attribute inside that identifier.
/// This record keeps the association, the `mapsId` path, the declared join
/// columns, and the target identifier columns until the derived-identifier phase
/// can validate and share the identifier columns.
///
/// @since 9.0
/// @author Steve Ebersole
public record DerivedIdentifierBinding(
		IdentifiableTypeMetadata ownerType,
		PersistentClass ownerBinding,
		Property property,
		ManyToOne value,
		EntityTypeBinder targetTypeBinder,
		boolean referenceToPrimaryKey,
		String mapsIdAttributeName,
		List<JoinColumn> joinColumns,
		List<Column> targetIdentifierColumns,
		ForeignKeySource foreignKeySource) {
}
