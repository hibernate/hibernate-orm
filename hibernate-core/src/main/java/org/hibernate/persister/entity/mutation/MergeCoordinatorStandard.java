/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableMergeBuilder;

/**
 * Specialized {@link UpdateCoordinator} for {@code merge into}.
 *
 * @author Gavin King
 */
public class MergeCoordinatorStandard extends UpdateCoordinatorStandard {

	public MergeCoordinatorStandard(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Override
	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(EntityTableMapping tableMapping) {
		return new TableMergeBuilder<>( entityPersister(), tableMapping, factory() );
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
			boolean restrictToTemporalExcluded,
			Object rowId,
			boolean forceDynamicUpdate,
			SharedSessionContractImplementor session) {
		final var updateValuesAnalysis = super.analyzeUpdateValues(
				entity,
				values,
				oldVersion,
				oldValues,
				dirtyAttributeIndexes,
				inclusionChecker,
				lockingChecker,
				dirtinessChecker,
				restrictToTemporalExcluded,
				rowId,
				forceDynamicUpdate,
				session
		);
		if ( oldValues == null ) {
			final TableSet tablesNeedingUpdate = updateValuesAnalysis.getTablesNeedingUpdate();
			final TableSet tablesWithNonNullValues = updateValuesAnalysis.getTablesWithNonNullValues();
			final TableSet tablesWithPreviousNonNullValues = updateValuesAnalysis.getTablesWithPreviousNonNullValues();
			for ( var tableMapping : entityPersister().getTableMappings() ) {
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
