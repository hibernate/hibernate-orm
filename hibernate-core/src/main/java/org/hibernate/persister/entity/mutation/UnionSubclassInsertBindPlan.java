/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashMap;
import java.util.Map;

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
			InsertValuesAnalysisForDecomposer valuesAnalysis,
			boolean[] insertable,
			TableInclusionChecker tableInclusionChecker,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector) {
		super(
				entityPersister,
				entity,
				identifier,
				mergeColumnValues( valuesAnalysis ),
				insertable,
				tableInclusionChecker,
				action,
				generatedValuesCollector
		);
	}

	private static Map<ColumnDetails, Object> mergeColumnValues(InsertValuesAnalysisForDecomposer valuesAnalysis) {
		final Map<ColumnDetails, Object> combined = new HashMap<ColumnDetails, Object>();
		valuesAnalysis.getColumnValuesByTable().forEach( (tableName, columnValues) -> {
			combined.putAll( columnValues );
		} );
		return combined;
	}
}
