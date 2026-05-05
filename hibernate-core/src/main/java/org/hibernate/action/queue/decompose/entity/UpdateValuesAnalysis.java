/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.mutation.AttributeAnalysis;
import org.hibernate.persister.entity.mutation.TableSet;
import org.hibernate.sql.model.TableMapping;

import java.util.List;
import java.util.function.Function;


/**
 * Values analysis for update operations in the decomposer.
 * Tracks which tables have non-null values and which tables need updating.
 *
 * @author Steve Ebersole
 */
public class UpdateValuesAnalysis implements org.hibernate.persister.entity.mutation.UpdateValuesAnalysis {
	private final TableDescriptorSet tablesWithNonNullValues = new TableDescriptorSet();
	private final TableDescriptorSet tablesWithPreviousNonNullValues = new TableDescriptorSet();
	private final TableDescriptorSet tablesNeedingUpdate = new TableDescriptorSet();
	private final TableDescriptorSet tablesNeedingDynamicUpdate = new TableDescriptorSet();
	private final TableSet legacyTablesWithNonNullValues = new TableSet();
	private final TableSet legacyTablesWithPreviousNonNullValues = new TableSet();
	private final TableSet legacyTablesNeedingUpdate = new TableSet();
	private final TableSet legacyTablesNeedingDynamicUpdate = new TableSet();
	private final Function<EntityTableDescriptor, TableMapping> legacyTableMappingAccess;
	private final Object[] values;
	private final boolean[] dirtiness;
	private final boolean hasDirtyAttributes;

	public UpdateValuesAnalysis(
			GraphEntityMutationTarget mutationTarget,
			Object[] values,
			Object[] previousValues,
			int[] dirtyAttributeIndexes,
			Function<EntityTableDescriptor, TableMapping> legacyTableMappingAccess) {
		this.legacyTableMappingAccess = legacyTableMappingAccess;
		this.values = values;
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
				addTable( table, tablesWithNonNullValues, legacyTablesWithNonNullValues );
				checkForNonNull = false;
			}

			if ( previousValues == null ) {
				addTable( table, tablesWithPreviousNonNullValues, legacyTablesWithPreviousNonNullValues );
				checkForPreviousNonNull = false;
			}

			if ( dirtyAttributeIndexes == null ) {
				// No dirty tracking - update all tables with columns
				if ( !table.columns().isEmpty() ) {
					addTable( table, tablesNeedingUpdate, legacyTablesNeedingUpdate );
				}
				checkForDirtiness = false;
			}

			for ( int i = 0; i < table.attributes().size(); i++ ) {
				var attribute = table.attributes().get( i );

				if ( checkForNonNull ) {
					if ( values[attribute.getStateArrayPosition()] != null ) {
						addTable( table, tablesWithNonNullValues, legacyTablesWithNonNullValues );
					}
				}

				if ( checkForPreviousNonNull ) {
					if ( previousValues[attribute.getStateArrayPosition()] != null ) {
						addTable( table, tablesWithPreviousNonNullValues, legacyTablesWithPreviousNonNullValues );
					}
				}

				if ( checkForDirtiness ) {
					if ( dirtiness[attribute.getStateArrayPosition()] ) {
						addTable( table, tablesNeedingUpdate, legacyTablesNeedingUpdate );
					}
				}
			}
		} );
	}

	private void addTable(EntityTableDescriptor table, TableDescriptorSet graphSet, TableSet legacySet) {
		graphSet.add( table );
		legacySet.add( legacyTableMappingAccess.apply( table ) );
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

	@Override
	public Object[] getValues() {
		return values;
	}

	@Override
	public TableSet getTablesNeedingUpdate() {
		return legacyTablesNeedingUpdate;
	}

	@Override
	public TableSet getTablesWithNonNullValues() {
		return legacyTablesWithNonNullValues;
	}

	@Override
	public TableSet getTablesWithPreviousNonNullValues() {
		return legacyTablesWithPreviousNonNullValues;
	}

	@Override
	public TableSet getTablesNeedingDynamicUpdate() {
		return legacyTablesNeedingDynamicUpdate;
	}

	@Override
	public List<AttributeAnalysis> getAttributeAnalyses() {
		return List.of();
	}
}
