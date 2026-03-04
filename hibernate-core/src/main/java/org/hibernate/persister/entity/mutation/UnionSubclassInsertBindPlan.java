/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

/// Specialized BindPlan for union subclass insert operations.
///
/// Union subclass tables contain ALL columns (inherited + subclass-specific),
/// so we bind all attributes without checking table containment.
///
/// @author Steve Ebersole
public class UnionSubclassInsertBindPlan extends InsertBindPlan {

	public UnionSubclassInsertBindPlan(
			EntityPersister entityPersister,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] insertable,
			InsertValuesAnalysis valuesAnalysis,
			TableInclusionChecker tableInclusionChecker,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector) {
		super(
				entityPersister,
				entity,
				identifier,
				state,
				insertable,
				valuesAnalysis,
				tableInclusionChecker,
				action,
				generatedValuesCollector
		);
	}

	@Override
	protected void decomposeAttribute(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
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
					if ( selectableMapping.isInsertable() ) {
						bindings.bindValue(
								jdbcValue,
								tableDetails.getTableName(), // Use current table name
								selectableMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}
}
