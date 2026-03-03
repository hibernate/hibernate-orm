/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.sql.model.TableMapping;

import java.util.HashSet;
import java.util.Set;

/**
 * Values analysis for update operations in the decomposer.
 * Tracks which tables have non-null values and which tables need updating.
 *
 * @author Steve Ebersole
 */
public class UpdateValuesAnalysisForDecomposer {
	private final Set<TableMapping> tablesWithNonNullValues = new HashSet<>();
	private final Set<TableMapping> tablesWithPreviousNonNullValues = new HashSet<>();
	private final Set<TableMapping> tablesNeedingUpdate = new HashSet<>();
	private final int[] dirtyAttributeIndexes;

	public UpdateValuesAnalysisForDecomposer(
			EntityMutationTarget mutationTarget,
			Object[] values,
			Object[] previousValues,
			int[] dirtyAttributeIndexes) {
		this.dirtyAttributeIndexes = dirtyAttributeIndexes;

		mutationTarget.forEachMutableTable( (tableMapping) -> {
			final int[] attributeIndexes = tableMapping.getAttributeIndexes();

			// Check current values for non-null
			if ( values == null ) {
				tablesWithNonNullValues.add( tableMapping );
			}
			else {
				for ( int i = 0; i < attributeIndexes.length; i++ ) {
					final int attributeIndex = attributeIndexes[i];
					if ( values[attributeIndex] != null ) {
						tablesWithNonNullValues.add( tableMapping );
						break;
					}
				}
			}

			// Check previous values for non-null
			if ( previousValues == null ) {
				tablesWithPreviousNonNullValues.add( tableMapping );
			}
			else {
				for ( int i = 0; i < attributeIndexes.length; i++ ) {
					final int attributeIndex = attributeIndexes[i];
					if ( previousValues[attributeIndex] != null ) {
						tablesWithPreviousNonNullValues.add( tableMapping );
						break;
					}
				}
			}

			// Determine if table needs updating
			if ( dirtyAttributeIndexes == null ) {
				// No dirty tracking - update all tables with columns
				if ( tableMapping.hasColumns() ) {
					tablesNeedingUpdate.add( tableMapping );
				}
			}
			else {
				// Check if any dirty attributes belong to this table
				for ( int dirtyIndex : dirtyAttributeIndexes ) {
					for ( int tableAttrIndex : attributeIndexes ) {
						if ( dirtyIndex == tableAttrIndex ) {
							tablesNeedingUpdate.add( tableMapping );
							break;
						}
					}
				}
			}
		} );
	}

	public boolean hasNonNullValues(TableMapping tableMapping) {
		return tablesWithNonNullValues.contains( tableMapping );
	}

	public boolean hasPreviousNonNullValues(TableMapping tableMapping) {
		return tablesWithPreviousNonNullValues.contains( tableMapping );
	}

	public boolean needsUpdate(TableMapping tableMapping) {
		return tablesNeedingUpdate.contains( tableMapping );
	}

	public boolean hasDirtyAttributes() {
		return dirtyAttributeIndexes != null && dirtyAttributeIndexes.length > 0;
	}
}
