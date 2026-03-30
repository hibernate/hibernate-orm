/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;


/**
 * Values analysis for update operations in the decomposer.
 * Tracks which tables have non-null values and which tables need updating.
 *
 * @author Steve Ebersole
 */
public class UpdateValuesAnalysisForDecomposer {
	private final TableDescriptorSet tablesWithNonNullValues = new TableDescriptorSet();
	private final TableDescriptorSet tablesWithPreviousNonNullValues = new TableDescriptorSet();
	private final TableDescriptorSet tablesNeedingUpdate = new TableDescriptorSet();
	private final TableDescriptorSet tablesNeedingDynamicUpdate = new TableDescriptorSet();
	private final boolean[] dirtiness;
	private final boolean hasDirtyAttributes;

	public UpdateValuesAnalysisForDecomposer(
			EntityMutationTarget mutationTarget,
			Object[] values,
			Object[] previousValues,
			int[] dirtyAttributeIndexes) {
		if ( dirtyAttributeIndexes == null ) {
			dirtiness = null;
			hasDirtyAttributes = false;
		}
		else {
			dirtiness = new boolean[mutationTarget.getTargetPart().getNumberOfAttributeMappings()];
			hasDirtyAttributes = dirtyAttributeIndexes.length > 0;
			if ( hasDirtyAttributes ) {
				for ( int i = 0; i < dirtyAttributeIndexes.length; i++ ) {
					dirtiness[dirtyAttributeIndexes[i]] = true;
				}
			}
		}

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
					if ( dirtiness[attribute.getStateArrayPosition()] ) {
						tablesNeedingUpdate.add( table );
					}
				}
			}
		} );
	}

	private boolean[] buildDirtinessArray(EntityMappingType targetPart, int[] dirtyAttributeIndexes) {
		return dirtiness;
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

	public boolean[] getDirtiness() {
		return dirtiness;
	}

	public boolean hasDirtyAttributes() {
		return hasDirtyAttributes;
	}
}
