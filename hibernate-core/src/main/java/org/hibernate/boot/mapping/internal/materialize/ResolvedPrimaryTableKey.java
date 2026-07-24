/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/// Resolved primary-key materialization input for an entity primary table.
///
/// This product carries the ordered identifier-column facts needed to
/// materialize a primary-table [PrimaryKey] without
/// rediscovering state from mutable mapping objects.
///
/// @author Steve Ebersole
/// @since 9.0
public record ResolvedPrimaryTableKey(
		PersistentClass entityBinding,
		Table table,
		MetadataBuildingContext buildingContext,
		EntityIdentifierBindingView identifierBindingView,
		List<Column> identifierColumns) {
	public ResolvedPrimaryTableKey(
			PersistentClass entityBinding,
			Table table,
			MetadataBuildingContext buildingContext) {
		this( entityBinding, table, buildingContext, null, List.of() );
	}

	public ResolvedPrimaryTableKey(
			PersistentClass entityBinding,
			Table table,
			MetadataBuildingContext buildingContext,
			@Nullable EntityIdentifierBindingView identifierBindingView,
			List<Column> identifierColumns) {
		this.entityBinding = entityBinding;
		this.table = table;
		this.buildingContext = buildingContext;
		this.identifierBindingView = identifierBindingView;
		this.identifierColumns = new ArrayList<>( identifierColumns );
	}


	@Override
	public @Nullable EntityIdentifierBindingView identifierBindingView() {
		return identifierBindingView;
	}


	public void addIdentifierColumn(Column column) {
		if ( !identifierColumns.contains( column ) ) {
			identifierColumns.add( column );
		}
	}
}
