/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT;

public class DB2zAggregateSupport extends DB2AggregateSupport {

	public static final AggregateSupport INSTANCE = new DB2zAggregateSupport();

	public DB2zAggregateSupport() {
		// No JSON support
		super( false );
	}

	@Override
	public String aggregateComponentCustomReadExpression(String template, String placeholder, String aggregateParentReadExpression, String columnExpression, int aggregateColumnTypeCode, SqlTypedMapping column, TypeConfiguration typeConfiguration) {
		if ( aggregateColumnTypeCode == STRUCT ) {
			throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
		}
		return super.aggregateComponentCustomReadExpression( template, placeholder, aggregateParentReadExpression,
				columnExpression, aggregateColumnTypeCode, column, typeConfiguration );
	}

	@Override
	public String aggregateComponentAssignmentExpression(String aggregateParentAssignmentExpression, String columnExpression, int aggregateColumnTypeCode, Column column) {
		if ( aggregateColumnTypeCode == STRUCT ) {
			throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
		}
		return super.aggregateComponentAssignmentExpression( aggregateParentAssignmentExpression, columnExpression,
				aggregateColumnTypeCode, column );
	}

	@Override
	public String aggregateCustomWriteExpression(AggregateColumn aggregateColumn, List<Column> aggregatedColumns) {
		final int sqlTypeCode = aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode();
		if ( (sqlTypeCode == ARRAY ? aggregateColumn.getTypeCode() : sqlTypeCode) == STRUCT ) {
			throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
		}
		return super.aggregateCustomWriteExpression( aggregateColumn, aggregatedColumns );
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(SelectableMapping aggregateColumn, SelectableMapping[] columnsToUpdate, TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		if ( aggregateSqlTypeCode == STRUCT ) {
			throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
		}
		return super.aggregateCustomWriteExpressionRenderer( aggregateColumn, columnsToUpdate, typeConfiguration );
	}
}
