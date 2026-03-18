/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.sql.model.ValuesAnalysis;

import java.util.Map;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class InsertValuesAnalysisForDecomposer implements ValuesAnalysis {
	private final Set<TableDescriptor> tablesWithNonNullValues = new IdentitySet<>();
	private final Map<String, Map<ColumnDetails, Object>> columnValues;

	public InsertValuesAnalysisForDecomposer(
			EntityGraphMutationTarget mutationTarget,
			Object[] values,
			SharedSessionContractImplementor session) {
		columnValues = CollectionHelper.mapOfSize( mutationTarget.getTableDescriptors().length );

		mutationTarget.forEachMutableTableDescriptor( (tableDescriptor) -> {
			final Map<ColumnDetails,Object> tableColumnValues = CollectionHelper.mapOfSize( tableDescriptor.columns().size() );
			columnValues.put( tableDescriptor.name(), tableColumnValues );
			tableDescriptor.attributes().forEach(  (attribute) -> {
				if ( !attribute.isPluralAttributeMapping() ) {
					var attributeValue = values[attribute.getStateArrayPosition()];
					attribute.breakDownJdbcValues(
							attributeValue,
							(valueIndex, jdbcValue, jdbcValueMapping) -> tableColumnValues.put(
									ColumnDetails.from( jdbcValueMapping, attribute.getStateArrayPosition(), attribute.getAttributeName() ),
									jdbcValue
							),
							session
					);

					if ( values[attribute.getStateArrayPosition()] != null ) {
						tablesWithNonNullValues.add( tableDescriptor );
					}
				}
			} );
		} );
	}

	public boolean hasNonNullBindings(TableDescriptor tableDescriptor) {
		return tablesWithNonNullValues.contains( tableDescriptor );
	}

	public Map<String, Map<ColumnDetails, Object>> getColumnValuesByTable() {
		return columnValues;
	}

	public Map<ColumnDetails, Object> getColumnValuesForTable(String tableName) {
		return columnValues.get( tableName );
	}
}
