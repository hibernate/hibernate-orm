/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.mutation.EntityTableMapping;

import java.util.List;

/// Descriptor for primary key columns.
///
/// @author Steve Ebersole
public record TableKeyDescriptor(List<ColumnDescriptor> columns) {

	public TableKeyDescriptor {
		columns = List.copyOf(columns);
	}

	public static TableKeyDescriptor from(SelectableMappings keyMappings) {
		var columns = CollectionHelper.<ColumnDescriptor>arrayList( keyMappings.getJdbcTypeCount() );
		keyMappings.forEachSelectable( (index, selectableMapping) -> {
			columns.add( ColumnDescriptor.from( selectableMapping ) );
		} );
		return new TableKeyDescriptor( columns );
	}

	public static TableKeyDescriptor from(EntityTableMapping.KeyMapping keyMapping) {
		var columns = CollectionHelper.<ColumnDescriptor>arrayList( keyMapping.getColumnCount() );

		keyMapping.forEachKeyColumn((index, selectable) -> {
			if (!selectable.isFormula()) {
				columns.add( ColumnDescriptor.from( selectable ) );
			}
		});

		return new TableKeyDescriptor(columns);
	}
}
