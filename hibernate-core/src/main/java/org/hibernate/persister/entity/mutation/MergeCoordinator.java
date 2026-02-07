/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableMergeBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;

/**
 * Specialized {@link UpdateCoordinator} for {@code merge into}.
 *
 * @author Gavin King
 */
public class MergeCoordinator extends UpdateCoordinatorStandard {

	public MergeCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super(entityPersister, factory);
	}

	@Override
	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(EntityTableMapping tableMapping) {
		return new TableMergeBuilder<>( entityPersister(), tableMapping, factory() );
	}

	@Override
	protected boolean isColumnIncludedInSet(SelectableMapping selectable) {
		return selectable.isUpdateable() || selectable.isInsertable();
	}

	private static boolean isInsertableOrUpdatable(AttributeMapping attribute) {
		final var attributeMetadata = attribute.getAttributeMetadata();
		return attributeMetadata.isUpdatable()
			|| attributeMetadata.isInsertable();
	}

	@Override
	protected InclusionChecker createInclusionChecker(boolean[] attributeUpdateability) {
		return (position, attribute) -> isInsertableOrUpdatable( attribute );
	}

	@Override
	protected boolean includeInStaticUpdate(
			int index,
			AttributeMapping attribute,
			boolean[] propertyUpdateability) {
		return isInsertableOrUpdatable( attribute )
			|| super.includeInStaticUpdate( index, attribute, propertyUpdateability );
	}

	@Override
	protected boolean includeProperty(boolean[] insertability, boolean[] updateability, int property) {
		return insertability[property] || updateability[property];
	}

	@Override
	public boolean[] getPropertyUpdateability(Object entity) {
		final boolean[] updateability = super.getPropertyUpdateability( entity );
		final boolean[] insertability = entityPersister().getPropertyInsertability();
		final var result = new boolean[updateability.length];
		for ( int i = 0; i < updateability.length; i++ ) {
			result[i] = updateability[i] || insertability[i];
		}
		return result;
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		final boolean[] updateability = entityPersister().getPropertyUpdateability();
		final boolean[] insertability = entityPersister().getPropertyInsertability();
		final var result = new boolean[updateability.length];
		for ( int i = 0; i < updateability.length; i++ ) {
			result[i] = updateability[i] || insertability[i];
		}
		return result;
	}
	@Override
	protected void forEachUpdatable(AttributeMapping attributeMapping, TableUpdateBuilder<?> tableUpdateBuilder) {
		attributeMapping.forEachSelectable( tableUpdateBuilder );
	}

	@Override
	protected UpdateValuesAnalysisImpl analyzeUpdateValues(
			Object entity,
			Object[] values,
			Object oldVersion,
			Object[] oldValues,
			int[] dirtyAttributeIndexes,
			InclusionChecker inclusionChecker,
			InclusionChecker lockingChecker,
			InclusionChecker dirtinessChecker,
			Object rowId,
			boolean forceDynamicUpdate,
			SharedSessionContractImplementor session) {
		final UpdateValuesAnalysisImpl updateValuesAnalysis = super.analyzeUpdateValues(
				entity,
				values,
				oldVersion,
				oldValues,
				dirtyAttributeIndexes,
				inclusionChecker,
				lockingChecker,
				dirtinessChecker,
				rowId,
				forceDynamicUpdate,
				session
		);
		if ( oldValues == null ) {
			final TableSet tablesNeedingUpdate = updateValuesAnalysis.getTablesNeedingUpdate();
			final TableSet tablesWithNonNullValues = updateValuesAnalysis.getTablesWithNonNullValues();
			final TableSet tablesWithPreviousNonNullValues = updateValuesAnalysis.getTablesWithPreviousNonNullValues();
			for ( EntityTableMapping tableMapping : entityPersister().getTableMappings() ) {
				// Need to upsert into all non-optional table mappings
				if ( !tableMapping.isOptional() ) {
					// If the table was previously not needing an update, remove it from tablesWithPreviousNonNullValues
					// to avoid triggering a delete-statement for this operation
					if ( !tablesNeedingUpdate.contains( tableMapping ) ) {
						tablesWithPreviousNonNullValues.remove( tableMapping );
					}
					tablesNeedingUpdate.add( tableMapping );
					tablesWithNonNullValues.add( tableMapping );
				}
			}
		}
		return updateValuesAnalysis;
	}
}
