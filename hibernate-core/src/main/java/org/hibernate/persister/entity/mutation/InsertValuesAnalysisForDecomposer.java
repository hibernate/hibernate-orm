/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.queue.Helper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;

import java.util.Map;
import java.util.Set;

import static org.hibernate.action.queue.Helper.normalizeTableName;

/**
 * @author Steve Ebersole
 */
public class InsertValuesAnalysisForDecomposer implements ValuesAnalysis {

	// todo : updated normalizer rules -
	//		Tables
	//			- 1) if quoted (``, "", []), strip quotes but leave text alone
	//			- 2) otherwise, lower case
	//		Columns
	//			- 1) if quoted (``, "", []), strip quotes but leave text alone
	//			- 2) otherwise, lowercase
	//			- 3) regardless of (1) / (2), always strip qualifier if one

	private final Set<TableMapping> tablesWithNonNullValues = new IdentitySet<>();
	private final Map<String, Map<ColumnDetails, Object>> columnValues;

	public InsertValuesAnalysisForDecomposer(
			EntityMutationTarget mutationTarget,
			Object[] values,
			SharedSessionContractImplementor session) {
		final EntityMappingType entityMapping = mutationTarget.getTargetPart();
		columnValues = CollectionHelper.mapOfSize( mutationTarget.getTableMappings().length );

		mutationTarget.forEachMutableTable( (tableMapping) -> {
			final int[] tableAttributeIndexes = tableMapping.getAttributeIndexes();

			final Map<ColumnDetails,Object> tableColumnValues = CollectionHelper.mapOfSize( tableAttributeIndexes.length );
			columnValues.put( Helper.normalizeTableName( tableMapping.getTableName() ), tableColumnValues );

			for ( int i = 0; i < tableAttributeIndexes.length; i++ ) {
				final AttributeMapping attribute = entityMapping.getAttributeMapping( tableAttributeIndexes[i] );
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

					if ( values[tableAttributeIndexes[i]] != null ) {
						tablesWithNonNullValues.add( tableMapping );
					}
				}
			}
		} );


	}

	public boolean hasNonNullBindings(TableMapping tableMapping) {
		return tablesWithNonNullValues.contains( tableMapping );
	}

	public Map<String, Map<ColumnDetails, Object>> getColumnValuesByTable() {
		return columnValues;
	}

	public Map<ColumnDetails, Object> getColumnValuesForTable(String tableName) {
		assert normalizeTableName( tableName ).equals( tableName );
		return columnValues.get( tableName );
	}
}
