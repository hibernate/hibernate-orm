/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import java.util.List;

import org.hibernate.boot.models.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.models.mapping.internal.categorize.KeyMapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;

/// Local binding state for an entity hierarchy identifier.
///
/// This captures the identifier shape produced by the identifier phase so later
/// phases can consume it directly rather than re-reading partially-bound state
/// from the metadata collector or retrying generic second-pass callbacks.
///
/// The [#columns()] list is intentionally ordered.  Table keys, association
/// identifiers, derived identifiers, and implicit join columns all need the
/// identifier column order selected while binding the root hierarchy id.  The
/// list remains live through the association-identifier phase because
/// association-valued `IdClass` attributes contribute their columns after the
/// initial identifier component is created.
///
/// @since 9.0
/// @author Steve Ebersole
public record IdentifierBinding(
		EntityTypeMetadata entityType,
		RootClass rootClass,
		KeyMapping keyMapping,
		KeyValue value,
		Property property,
		Table table,
		List<Column> columns) {
}
