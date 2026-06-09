/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
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
/// identifier column order selected while binding the root hierarchy id.
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
	public IdentifierBinding {
		columns = List.copyOf( columns );
	}
}
