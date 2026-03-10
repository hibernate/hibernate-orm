/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

/// Specialized BindPlan for union subclass update operations.
///
/// Union subclass tables contain ALL columns (inherited + subclass-specific),
/// so we bind all attributes without checking table containment.
///
/// @author Steve Ebersole
public class UnionSubclassUpdateBindPlan extends UpdateBindPlan {

	public UnionSubclassUpdateBindPlan(
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis) {
		super(
				entityPersister,
				entity,
				identifier,
				rowId,
				state,
				previousState,
				version,
				dirtyFields,
				updateable,
				applyOptimisticLocking,
				valuesAnalysis
		);
	}

	@Override
	protected void decomposeAttributeForSet(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			String tableName,
			AttributeMapping mapping) {
		if ( mapping instanceof PluralAttributeMapping ) {
			return;
		}

		// Union subclass tables contain ALL columns (inherited + specific)
		// so we don't check getContainingTableExpression()

		mapping.decompose(
				value,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								tableName,
								selectableMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}
}
