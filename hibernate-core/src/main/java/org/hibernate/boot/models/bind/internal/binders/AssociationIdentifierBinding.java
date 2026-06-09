/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import jakarta.persistence.JoinColumn;

/// Pending binding for an association-valued `IdClass` identifier attribute.
///
/// During the identifier phase, a non-aggregated identifier may include an
/// owning to-one attribute.  The `ManyToOne` value and component property can be
/// created immediately, but the join columns depend on the target entity's
/// identifier binding.  This record carries the source join columns, target type
/// binder, and mutable identifier-column list to the association-identifier
/// phase, where the target identifier order is known.
///
/// @since 9.0
/// @author Steve Ebersole
public record AssociationIdentifierBinding(
		EntityTypeMetadata ownerType,
		PersistentClass ownerBinding,
		Property property,
		ManyToOne value,
		EntityTypeBinder targetTypeBinder,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource,
		List<Column> identifierColumns) {
}
