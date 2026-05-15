/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.GraphEntityMutationTarget;

import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.mutation.AttributeAnalysis;
import org.hibernate.persister.entity.mutation.TableSet;
import org.hibernate.sql.model.TableMapping;

import java.util.BitSet;
import java.util.List;
import java.util.function.Function;


/// Values analysis for update operations in the decomposer.
/// Tracks which tables have non-null values and which tables need updating.
///
/// @author Steve Ebersole
public class UpdateValuesAnalysis implements org.hibernate.persister.entity.mutation.UpdateValuesAnalysis {
	private final GraphEntityMutationTarget mutationTarget;
	private final Function<EntityTableDescriptor, TableMapping> legacyTableMappingAccess;
	private BitSet tablesWithNonNullValues;
	private BitSet tablesWithPreviousNonNullValues;
	private BitSet tablesNeedingUpdate;
	private BitSet tablesNeedingDynamicUpdate;
	private TableSet legacyTablesWithNonNullValues;
	private TableSet legacyTablesWithPreviousNonNullValues;
	private TableSet legacyTablesNeedingUpdate;
	private TableSet legacyTablesNeedingDynamicUpdate;
	private final Object[] values;
	private final boolean[] dirtiness;
	private final boolean hasDirtyAttributes;

	public UpdateValuesAnalysis(
			GraphEntityMutationTarget mutationTarget,
			Object[] values,
			Object[] previousValues,
			int[] dirtyAttributeIndexes,
			Function<EntityTableDescriptor, TableMapping> legacyTableMappingAccess) {
		this.mutationTarget = mutationTarget;
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
				addTableWithNonNullValues( table );
				checkForNonNull = false;
			}

			if ( previousValues == null ) {
				addTableWithPreviousNonNullValues( table );
				checkForPreviousNonNull = false;
			}

			if ( dirtyAttributeIndexes == null ) {
				// No dirty tracking - update all tables with columns
				if ( !table.columns().isEmpty() ) {
					addTableNeedingUpdate( table );
				}
				checkForDirtiness = false;
			}

			for ( int i = 0; i < table.attributes().size(); i++ ) {
				var attribute = table.attributes().get( i );

				if ( checkForNonNull ) {
					if ( values[attribute.getStateArrayPosition()] != null ) {
						addTableWithNonNullValues( table );
					}
				}

				if ( checkForPreviousNonNull ) {
					if ( previousValues[attribute.getStateArrayPosition()] != null ) {
						addTableWithPreviousNonNullValues( table );
					}
				}

				if ( checkForDirtiness ) {
					if ( dirtiness[attribute.getStateArrayPosition()] ) {
						addTableNeedingUpdate( table );
					}
				}
			}
		} );
	}

	private void addTableWithNonNullValues(EntityTableDescriptor table) {
		tablesWithNonNullValues = addTable( table, tablesWithNonNullValues );
	}

	private void addTableWithPreviousNonNullValues(EntityTableDescriptor table) {
		tablesWithPreviousNonNullValues = addTable( table, tablesWithPreviousNonNullValues );
	}

	private void addTableNeedingUpdate(EntityTableDescriptor table) {
		tablesNeedingUpdate = addTable( table, tablesNeedingUpdate );
	}

	private BitSet addTable(EntityTableDescriptor table, BitSet graphSet) {
		if ( graphSet == null ) {
			graphSet = new BitSet();
		}
		graphSet.set( table.getRelativePosition() );
		return graphSet;
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
		return contains( tablesWithNonNullValues, table );
	}

	public boolean hasPreviousNonNullValues(EntityTableDescriptor table) {
		return contains( tablesWithPreviousNonNullValues, table );
	}

	public boolean needsUpdate(EntityTableDescriptor table) {
		return contains( tablesNeedingUpdate, table );
	}

	private boolean contains(BitSet graphSet, EntityTableDescriptor table) {
		return graphSet != null && graphSet.get( table.getRelativePosition() );
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
		if ( legacyTablesNeedingUpdate == null ) {
			legacyTablesNeedingUpdate = buildLegacyTableSet( tablesNeedingUpdate );
		}
		return legacyTablesNeedingUpdate;
	}

	@Override
	public TableSet getTablesWithNonNullValues() {
		if ( legacyTablesWithNonNullValues == null ) {
			legacyTablesWithNonNullValues = buildLegacyTableSet( tablesWithNonNullValues );
		}
		return legacyTablesWithNonNullValues;
	}

	@Override
	public TableSet getTablesWithPreviousNonNullValues() {
		if ( legacyTablesWithPreviousNonNullValues == null ) {
			legacyTablesWithPreviousNonNullValues = buildLegacyTableSet( tablesWithPreviousNonNullValues );
		}
		return legacyTablesWithPreviousNonNullValues;
	}

	@Override
	public TableSet getTablesNeedingDynamicUpdate() {
		if ( legacyTablesNeedingDynamicUpdate == null ) {
			legacyTablesNeedingDynamicUpdate = buildLegacyTableSet( tablesNeedingDynamicUpdate );
		}
		return legacyTablesNeedingDynamicUpdate;
	}

	private TableSet buildLegacyTableSet(BitSet graphSet) {
		final TableSet legacySet = new TableSet();
		if ( graphSet != null ) {
			mutationTarget.forEachMutableTableDescriptor( table -> {
				if ( graphSet.get( table.getRelativePosition() ) ) {
					legacySet.add( legacyTableMappingAccess.apply( table ) );
				}
			} );
		}
		return legacySet;
	}

	@Override
	public List<AttributeAnalysis> getAttributeAnalyses() {
		return List.of();
	}
}
