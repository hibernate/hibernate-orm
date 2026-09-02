/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.internal;


import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.aggregate.spi.AggregateAuxiliaryObject;
import org.hibernate.dialect.aggregate.spi.AggregateAuxiliaryObjectRequest;
import org.hibernate.dialect.aggregate.spi.AggregateComponentAssignmentRequest;
import org.hibernate.dialect.aggregate.spi.AggregateComponentReadRequest;
import org.hibernate.dialect.aggregate.spi.AggregateCustomWriteRequest;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.spi.AggregateWriteExpressionRenderer;
import org.hibernate.dialect.aggregate.spi.AggregateWriteRendererRequest;
import org.hibernate.dialect.aggregate.spi.StandardAggregateSupport;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

public class AggregateSupportImpl extends StandardAggregateSupport {

	public static final AggregateSupport INSTANCE = new AggregateSupportImpl();

	protected interface AggregateColumnWriteExpression
			extends org.hibernate.dialect.aggregate.spi.AggregateColumnWriteExpression {
	}

	protected interface WriteExpressionRenderer extends AggregateWriteExpressionRenderer {
		void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier);

		@Override
		default void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				org.hibernate.dialect.aggregate.spi.AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier) {
			render(
					sqlAppender,
					translator,
					aggregateColumnWriteExpression::getValueExpression,
					qualifier
			);
		}
	}

	@Override
	public String aggregateComponentCustomReadExpression(AggregateComponentReadRequest request) {
		return aggregateComponentCustomReadExpression(
				request.template(),
				request.placeholder(),
				request.aggregateParentReadExpression(),
				request.columnExpression(),
				request.aggregateColumnTypeCode(),
				request.column(),
				request.typeConfiguration()
		);
	}

	@Override
	public String aggregateComponentAssignmentExpression(AggregateComponentAssignmentRequest request) {
		return aggregateComponentAssignmentExpression(
				request.aggregateParentAssignmentExpression(),
				request.columnExpression(),
				request.aggregateColumnTypeCode(),
				AggregateColumnDescriptorAdapter.column( request.column() )
		);
	}

	@Override
	public String aggregateCustomWriteExpression(AggregateCustomWriteRequest request) {
		return aggregateCustomWriteExpression(
				AggregateColumnDescriptorAdapter.aggregateColumn( request.aggregateColumn() ),
				AggregateColumnDescriptorAdapter.columns( request.components() )
		);
	}

	@Override
	public AggregateWriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			AggregateWriteRendererRequest request) {
		return aggregateCustomWriteExpressionRenderer(
				request.aggregateColumn(),
				request.columnsToUpdate().toArray( SelectableMapping[]::new ),
				request.typeConfiguration()
		);
	}

	@Override
	public List<AggregateAuxiliaryObject> aggregateAuxiliaryObjects(AggregateAuxiliaryObjectRequest request) {
		return List.of();
	}

	public String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			SqlTypedMapping column,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Dialect does not support aggregateComponentCustomReadExpression: " + getClass().getName() );
	}

	public String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			Column column) {
		throw new UnsupportedOperationException( "Dialect does not support aggregateComponentAssignmentExpression: " + getClass().getName() );
	}

	public String aggregateCustomWriteExpression(
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		return null;
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		throw new UnsupportedOperationException( "Dialect does not support requiresAggregateCustomWriteExpressionRenderer: " + getClass().getName() );
	}

	@Override
	public boolean preferSelectAggregateMapping(int aggregateSqlTypeCode) {
		// By default, assume the driver supports this and prefer selecting the aggregate column
		return true;
	}

	@Override
	public boolean preferBindAggregateMapping(int aggregateSqlTypeCode) {
		// By default, assume the driver supports this and prefer binding the aggregate column
		return true;
	}

	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Dialect does not support aggregateCustomWriteExpressionRenderer: " + getClass().getName() );
	}

	public List<AuxiliaryDatabaseObject> aggregateAuxiliaryDatabaseObjects(
			Namespace namespace,
			String aggregatePath,
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		return Collections.emptyList();
	}

	@Override
	public int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode) {
		return switch (aggregateColumnSqlTypeCode) {
			case JSON -> columnSqlTypeCode == ARRAY ? JSON_ARRAY : columnSqlTypeCode;
			case SQLXML -> columnSqlTypeCode == ARRAY ? XML_ARRAY : columnSqlTypeCode;
			default -> columnSqlTypeCode;
		};
	}

	@Override
	public boolean supportsComponentCheckConstraints() {
		return true;
	}
}
