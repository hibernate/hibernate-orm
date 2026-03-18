/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.metamodel.mapping.AttributeMapping;

import java.util.HashSet;
import java.util.Set;

/**
 * Values analysis for update operations in the decomposer.
 * Tracks which tables have non-null values and which tables need updating.
 *
 * @author Steve Ebersole
 */
public class UpdateValuesAnalysisForDecomposer {
	private final Set<EntityTableDescriptor> tablesWithNonNullValues = new HashSet<>();
	private final Set<EntityTableDescriptor> tablesWithPreviousNonNullValues = new HashSet<>();
	private final Set<EntityTableDescriptor> tablesNeedingUpdate = new HashSet<>();
	private final int[] dirtyAttributeIndexes;

	public UpdateValuesAnalysisForDecomposer(
			EntityGraphMutationTarget mutationTarget,
			Object[] values,
			Object[] previousValues,
			int[] dirtyAttributeIndexes) {
		this.dirtyAttributeIndexes = dirtyAttributeIndexes;

		mutationTarget.forEachMutableTableDescriptor( (table) -> {
			boolean checkForNonNull = true;
			boolean checkForPreviousNonNull = true;
			boolean checkForDirtiness = true;

			if ( values == null ) {
				tablesWithNonNullValues.add( table );
				checkForNonNull = false;
			}

			if ( previousValues == null ) {
				tablesWithPreviousNonNullValues.add( table );
				checkForPreviousNonNull = false;
			}

			if ( dirtyAttributeIndexes == null ) {
				// No dirty tracking - update all tables with columns
				if ( !table.columns().isEmpty() ) {
					tablesNeedingUpdate.add( table );
				}
				checkForDirtiness = false;
			}

			for ( int i = 0; i < table.attributes().size(); i++ ) {
				var attribute = table.attributes().get( i );

				if ( checkForNonNull ) {
					if ( values[attribute.getStateArrayPosition()] != null ) {
						tablesWithNonNullValues.add( table );
					}
				}

				if ( checkForPreviousNonNull ) {
					if ( previousValues[attribute.getStateArrayPosition()] != null ) {
						tablesWithPreviousNonNullValues.add( table );
					}
				}

				if ( checkForDirtiness ) {
					if ( isAttributeDirty( attribute, dirtyAttributeIndexes ) ) {
						tablesNeedingUpdate.add( table );
					}
				}
			}
		} );
	}

	private boolean isAttributeDirty(AttributeMapping attribute, int[] dirtyAttributeIndexes) {
		for ( int i = 0; i < dirtyAttributeIndexes.length; i++ ) {
			if ( attribute.getStateArrayPosition() == dirtyAttributeIndexes[i] ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNonNullValues(EntityTableDescriptor table) {
		return tablesWithNonNullValues.contains( table );
	}

	public boolean hasPreviousNonNullValues(EntityTableDescriptor table) {
		return tablesWithPreviousNonNullValues.contains( table );
	}

	public boolean needsUpdate(EntityTableDescriptor table) {
		return tablesNeedingUpdate.contains( table );
	}

	public boolean hasDirtyAttributes() {
		return dirtyAttributeIndexes != null && dirtyAttributeIndexes.length > 0;
	}
}
