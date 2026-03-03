/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

import java.util.ArrayList;
import java.util.List;

/// ValuesAnalysis for entity insert operations
///
/// @author Steve Ebersole
public class InsertValuesAnalysis implements ValuesAnalysis {
	private final List<TableMapping> tablesWithNonNullValues = new ArrayList<>();

	public InsertValuesAnalysis(EntityMutationTarget mutationTarget, Object[] values) {
		mutationTarget.forEachMutableTable( (tableMapping) -> {
			final int[] tableAttributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < tableAttributeIndexes.length; i++ ) {
				if ( values[tableAttributeIndexes[i]] != null ) {
					tablesWithNonNullValues.add( tableMapping );
					break;
				}
			}
		} );
	}

	public boolean hasNonNullBindings(TableMapping tableMapping) {
		return tablesWithNonNullValues.contains( tableMapping );
	}
}
