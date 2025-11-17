/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A set of operations providing support for aggregate column types
 * in a certain {@linkplain Dialect SQL dialect}.
 *
 * @since 6.2
 */
@Incubating
public interface AggregateSupport {

	/**
	 * Returns the custom read expression to use for {@code column}.
	 * Replaces the given {@code placeholder} in the given {@code template}
	 * by the custom read expression to use for {@code column}.
	 *
	 * @param template The custom read expression template of the column
	 * @param placeholder The placeholder to replace with the actual read expression
	 * @param aggregateParentReadExpression The expression to the aggregate column, which contains the column
	 * @param columnExpression The column within the aggregate type, for which to return the read expression
	 * @param aggregateColumn The type information for the aggregate column
	 * @param column The column within the aggregate type, for which to return the read expression
	 */
	default String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		final int sqlTypeCode = aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode();
		return aggregateComponentCustomReadExpression(
				template,
				placeholder,
				aggregateParentReadExpression,
				columnExpression,
				// We need to know what array this is STRUCT_ARRAY/JSON_ARRAY/XML_ARRAY,
				// which we can easily get from the type code of the aggregate column
				sqlTypeCode == SqlTypes.ARRAY ? aggregateColumn.getTypeCode() : sqlTypeCode,
				new SqlTypedMappingImpl(
						column.getTypeName(),
						column.getLength(),
						column.getArrayLength(),
						column.getPrecision(),
						column.getScale(),
						column.getTemporalPrecision(),
						column.getType()
				),
				aggregateColumn.getComponent().getMetadata().getTypeConfiguration()
		);
	}

	/**
	 * Returns the custom read expression to use for {@code column}.
	 * Replaces the given {@code placeholder} in the given {@code template}
	 * by the custom read expression to use for {@code column}.
	 *
	 * @param template The custom read expression template of the column
	 * @param placeholder The placeholder to replace with the actual read expression
	 * @param aggregateParentReadExpression The expression to the aggregate column, which contains the column
	 * @param columnExpression The column within the aggregate type, for which to return the read expression
	 * @param aggregateColumnTypeCode The SQL type code of the aggregate column
	 * @param column The column within the aggregate type, for which to return the read expression
	 * @param typeConfiguration The type configuration
	 * @since 7.0
	 */
	String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			SqlTypedMapping column,
			TypeConfiguration typeConfiguration);

	/**
	 * Returns the assignment expression to use for {@code column},
	 * which is part of the aggregate type of {@code aggregatePath}.
	 *
	 * @param aggregateParentAssignmentExpression The expression to the aggregate column, which contains the column
	 * @param columnExpression The column within the aggregate type, for which to return the assignment expression
	 * @param aggregateColumn The type information for the aggregate column
	 * @param column The column within the aggregate type, for which to return the assignment expression
	 */
	default String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		final int sqlTypeCode = aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode();
		return aggregateComponentAssignmentExpression(
				aggregateParentAssignmentExpression,
				columnExpression,
				// We need to know what array this is STRUCT_ARRAY/JSON_ARRAY/XML_ARRAY,
				// which we can easily get from the type code of the aggregate column
				sqlTypeCode == SqlTypes.ARRAY ? aggregateColumn.getTypeCode() : sqlTypeCode,
				column
		);
	}

	/**
	 * Returns the assignment expression to use for {@code column},
	 * which is part of the aggregate type of {@code aggregatePath}.
	 *
	 * @param aggregateParentAssignmentExpression The expression to the aggregate column, which contains the column
	 * @param columnExpression The column within the aggregate type, for which to return the assignment expression
	 * @param aggregateColumnTypeCode The SQL type code of the aggregate column
	 * @param column The column within the aggregate type, for which to return the assignment expression
	 *
	 * @since 7.0
	 */
	String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			Column column);

	/**
	 * Returns the custom write expression to use for an aggregate column
	 * of the given column type, containing the given aggregated columns.
	 *
	 * @param aggregateColumn The type information for the aggregate column
	 * @param aggregatedColumns The columns of the aggregate type
	 */
	String aggregateCustomWriteExpression(AggregateColumn aggregateColumn, List<Column> aggregatedColumns);

	/**
	 * Whether {@link #aggregateCustomWriteExpressionRenderer(SelectableMapping, SelectableMapping[], TypeConfiguration)} is needed
	 * when assigning an expression to individual aggregated columns in an update statement.
	 */
	boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode);

	/**
	 * Whether to prefer selecting the aggregate column as a whole instead of individual parts.
	 */
	boolean preferSelectAggregateMapping(int aggregateSqlTypeCode);
	/**
	 * Whether to prefer binding the aggregate column as a whole instead of individual parts.
	 */
	boolean preferBindAggregateMapping(int aggregateSqlTypeCode);

	/**
	 * @param aggregateColumn The mapping of the aggregate column
	 * @param columnsToUpdate The mappings of the columns that should be updated
	 * @param typeConfiguration The type configuration
	 */
	WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration);

	/**
	 * Contract for rendering the custom write expression that updates a selected set of aggregated columns
	 * within an aggregate column to the value expressions as given by the {@code aggregateColumnWriteExpression}.
	 */
	interface WriteExpressionRenderer {
		/**
		 * Renders the qualified custom write expression to the {@link SqlAppender} with the value expressions for each
		 * selectable as returned by {@link AggregateColumnWriteExpression#getValueExpression(SelectableMapping)}.
		 */
		void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier);
	}

	/**
	 * The actual write expression for an aggregate column
	 * which gives access to the value expressions for the respective selectable mapping.
	 */
	interface AggregateColumnWriteExpression {
		/**
		 * Returns the value expression to assign to the given selectable mapping,
		 * or throws an {@link IllegalArgumentException} when an invalid selectable mapping is passed.
		 */
		Expression getValueExpression(SelectableMapping selectableMapping);
	}

	/**
	 * Allows to generate auxiliary database objects for an aggregate type.
	 */
	List<AuxiliaryDatabaseObject> aggregateAuxiliaryDatabaseObjects(
			Namespace namespace,
			String aggregatePath,
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns);

	/**
	 * Returns the {@link org.hibernate.type.SqlTypes} type code to use for the given column type code,
	 * when aggregated within a column of the given aggregate column type code.
	 * Allows to change types when a database does not allow to use certain types within an aggregate type,
	 * like DB2 doesn't allow the use of {@code boolean} within an object/struct type.
	 *
	 * @param aggregateColumnSqlTypeCode The {@link org.hibernate.type.SqlTypes} type code of the aggregate column
	 * @param columnSqlTypeCode The {@link org.hibernate.type.SqlTypes} type code of the column
	 */
	int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode);

	/**
	 * Returns whether the database supports the use of a check constraint on tables,
	 * to implement not-null and other constraints of an aggregate type.
	 */
	boolean supportsComponentCheckConstraints();
}
