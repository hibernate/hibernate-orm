/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.sql.model.TableMapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Immutable descriptor for a table involved in a mutation.
///
/// Provides metadata needed for mutation execution.
///
/// @param name Table name from the mapping model.
/// @param relativePosition Position within the target's grouping of tables.
/// @param columns The columns contained on this table.
/// @param attributes The attributes mapped to this table.
/// @param attributeColumnIndexes A mapping of attribute to the indices (relative to [#columns()]) for the columns that attribute maps to.
///
/// @author Steve Ebersole
public record EntityTableDescriptor(
		String name,
		int relativePosition,
		boolean isIdentifierTable,
		boolean isOptional,
		boolean isInverse,
		boolean isSelfReferential,
		boolean hasUniqueConstraints,
		boolean cascadeDeleteEnabled,
		TableMapping.MutationDetails insertDetails,
		TableMapping.MutationDetails updateDetails,
		TableMapping.MutationDetails deleteDetails,
		List<ColumnDescriptor> columns,
		List<AttributeMapping> attributes,
		Map<AttributeMapping,List<Integer>> attributeColumnIndexes,
		TableKeyDescriptor keyDescriptor) implements TableDescriptor, Serializable {

	public EntityTableDescriptor {
		// Immutable - defensive copy
		columns = List.copyOf(columns);
	}

	public void forEachAttributeColumn(AttributeMapping attribute, Consumer<ColumnDescriptor> consumer) {
		var columnIndexes = attributeColumnIndexes().get( attribute );
		if ( columnIndexes == null ) {
			// Attribute has no columns mapped to this table (e.g., plural attributes, or attributes with no selectables)
			return;
		}
		//noinspection ForLoopReplaceableByForEach
		for ( int i1 = 0; i1 < columnIndexes.size(); i1++ ) {
			consumer.accept( columns().get( columnIndexes.get( i1 ) ) );
		}
	}

	public void forAllColumns(Consumer<ColumnDescriptor> consumer) {
		Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.forEach( consumer );
	}

	/// Find column by name.
	/// Returns null if not found.
	public ColumnDescriptor findColumn(String columnName) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter( col -> col.name().equals( columnName ) )
				.findFirst()
				.orElse( null );
	}

	/// Find columns by names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(String... columnNames) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter( (col) -> ArrayHelper.contains( columnNames, col.name() ) )
				.toList();
	}

	/// Find columns by names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(Collection<String> columnNames) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter(col -> columnNames.contains( col.name() ))
				.toList();
	}

	@Override
	public int getRelativePosition() {
		return relativePosition;
	}
}
