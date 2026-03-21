/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.support.TableInclusionChecker;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.sql.model.ValuesAnalysis;

import java.util.Set;

/**
 * Lightweight values analysis that only tracks which tables have non-null values.
 * Used when expensive column-value extraction is not needed.
 *
 * @author Steve Ebersole
 */
public class SimpleInsertValuesAnalysis implements ValuesAnalysis, TableInclusionChecker {
	private final Set<TableDescriptor> tablesWithNonNullValues = new IdentitySet<>();

	public SimpleInsertValuesAnalysis(EntityGraphMutationTarget mutationTarget, Object[] values) {
		mutationTarget.forEachMutableTableDescriptor( (tableDescriptor) -> {
			// Check if any attribute mapped to this table has a non-null value
			for ( var attribute : tableDescriptor.attributes() ) {
				if ( !attribute.isPluralAttributeMapping() ) {
					if ( values[attribute.getStateArrayPosition()] != null ) {
						tablesWithNonNullValues.add( tableDescriptor );
						break; // Found non-null value, move to next table
					}
				}
			}
		} );
	}

	public boolean hasNonNullBindings(TableDescriptor tableDescriptor) {
		return tablesWithNonNullValues.contains( tableDescriptor );
	}

	@Override
	public boolean include(TableDescriptor tableDescriptor) {
		// we want to actually do an insert into the table if either -
		// 		* the table is non-optional
		//		* we found non-null bindings (aka non-null values we need to insert)
		return !tableDescriptor.isOptional() || hasNonNullBindings( tableDescriptor );
	}
}
