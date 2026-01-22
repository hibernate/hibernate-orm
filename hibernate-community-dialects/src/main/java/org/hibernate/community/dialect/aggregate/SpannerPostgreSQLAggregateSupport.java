/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.aggregate;

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.PostgreSQLAggregateSupport;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;

/***
 * Spanner supports only JSON aggregation. It doesn't support XML or STRUCT aggregation
 */
public class SpannerPostgreSQLAggregateSupport extends PostgreSQLAggregateSupport {

	public static final AggregateSupport INSTANCE = new SpannerPostgreSQLAggregateSupport();

	@Override
	public String aggregateComponentCustomReadExpression(String template, String placeholder, String aggregateParentReadExpression, String columnExpression, int aggregateColumnTypeCode, SqlTypedMapping column, TypeConfiguration typeConfiguration) {
		return switch ( aggregateColumnTypeCode ) {
			case JSON_ARRAY, JSON ->
					super.aggregateComponentCustomReadExpression( template, placeholder, aggregateParentReadExpression,
							columnExpression, aggregateColumnTypeCode, column, typeConfiguration );
			default ->
					throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
		};
	}

	@Override
	public String aggregateComponentAssignmentExpression(String aggregateParentAssignmentExpression, String columnExpression, int aggregateColumnTypeCode, Column column) {
		return switch ( aggregateColumnTypeCode ) {
			case JSON, JSON_ARRAY ->
					super.aggregateComponentAssignmentExpression( aggregateParentAssignmentExpression, columnExpression,
							aggregateColumnTypeCode, column );
			default ->
					throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
		};
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == JSON;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(SelectableMapping aggregateColumn, SelectableMapping[] columnsToUpdate, TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		if ( aggregateSqlTypeCode == JSON ) {
			return super.aggregateCustomWriteExpressionRenderer( aggregateColumn, columnsToUpdate, typeConfiguration );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}
}
