/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.sql.model.TableMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/// Immutable descriptor for a table involved in a mutation.
///
/// All names are pre-normalized at construction time.
/// Provides metadata needed for mutation execution.
///
/// @param normalizedName Normalized table name - see [Helper#normalizeTableName(String)].
/// @param physicalName Name from the mapping model (non-normalized).
/// @param relativePosition Position within the target's grouping of tables.
/// @param columns The columns contained on this table.
/// @param attributes The attributes mapped to this table.
/// @param attributeColumnIndexes A mapping of attribute to the indices (relative to [#columns()]) for the columns that attribute maps to.
///
/// @author Steve Ebersole
public record EntityTableDescriptor(
		String normalizedName,
		String physicalName,
		int relativePosition,
		boolean isIdentifierTable,
		boolean isOptional,
		boolean isInverse,
		boolean cascadeDeleteEnabled,
		TableMapping.MutationDetails insertDetails,
		TableMapping.MutationDetails updateDetails,
		TableMapping.MutationDetails deleteDetails,
		List<ColumnDescriptor> columns,
		List<AttributeMapping> attributes,
		Map<AttributeMapping,List<Integer>> attributeColumnIndexes,
		TableKeyDescriptor keyDescriptor) implements TableDescriptor {

	public EntityTableDescriptor {
		// Immutable - defensive copy
		columns = List.copyOf(columns);
	}

	///Get the physical (non-normalized) table name.
	@Override
	public String physicalName() {
		return physicalName;
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

	/// Find column by normalized name.
	/// Returns null if not found.
	public ColumnDescriptor findColumn(String normalizedColumnName) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter( col -> col.normalizedName().equals( normalizedColumnName ) )
				.findFirst()
				.orElse( null );
	}

	/// Find columns by normalized names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(String... normalizedColumnNames) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter( (col) -> ArrayHelper.contains( normalizedColumnNames, col.normalizedName() ) )
				.toList();
	}

	/// Find columns by normalized names.
	/// Returns empty if none found.
	public List<ColumnDescriptor> findColumns(Collection<String> normalizedColumnNames) {
		return Stream.of(columns(), keyDescriptor().columns())
				.flatMap(List::stream)
				.filter(col -> normalizedColumnNames.contains( col.normalizedName() ))
				.toList();
	}

	/// Validate that all required columns exist.
	/// Throws clear exception if missing.
	public void validateColumns(Set<String> requiredNormalizedNames) {
		var missing = new ArrayList<String>();
		for (var required : requiredNormalizedNames) {
			if (findColumn(required) == null) {
				missing.add(required);
			}
		}
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException(
					"Table " + normalizedName + " missing columns: " + missing +
					" (available: " + columns.stream()
							.map( ColumnDescriptor::normalizedName)
							.toList() + ")"
			);
		}
	}
}
