/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableMergeBuilder;

/**
 * Specialized {@link UpdateCoordinator} for {@code merge into}.
 *
 * @author Gavin King
 */
public class MergeCoordinator extends UpdateCoordinatorStandard {

	public MergeCoordinator(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		super(entityPersister, factory);
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
