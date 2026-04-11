/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.persister.entity.mutation.EntityTableMappingImpl;

import java.io.Serializable;
import java.util.List;

/// Descriptor for primary key columns.
///
/// @author Steve Ebersole
public record TableKeyDescriptor(List<ColumnDescriptor> columns) implements SelectableMappings, Serializable {

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

	public static TableKeyDescriptor from(EntityTableMappingImpl.KeyMapping keyMapping) {
		var columns = CollectionHelper.<ColumnDescriptor>arrayList( keyMapping.getColumnCount() );

		keyMapping.forEachKeyColumn((index, selectable) -> {
			if (!selectable.isFormula()) {
				columns.add( ColumnDescriptor.from( selectable ) );
			}
		});

		return new TableKeyDescriptor(columns);
	}

	@Override
	public int getJdbcTypeCount() {
		return columns().size();
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return columns().get(columnIndex);
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		for ( int i = 0; i < columns.size(); i++ ) {
			consumer.accept( i, columns().get(i) );
		}
		return getJdbcTypeCount();
	}
}
