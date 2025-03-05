/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.model.ast.ColumnValueBinding;

/**
 *
 * Metadata about the indicator column for entities and collections enabled
 * for soft delete
 *
 * @see org.hibernate.annotations.SoftDelete
 *
 * @author Steve Ebersole
 */
public interface SoftDeleteMapping extends SelectableMapping, VirtualModelPart, SqlExpressible {
	String ROLE_NAME = "{soft-delete}";

	/**
	 * The soft-delete strategy - how to interpret indicator values
	 */
	SoftDeleteType getSoftDeleteStrategy();

	/**
	 * The name of the soft-delete indicator column.
	 */
	String getColumnName();

	/**
	 * The name of the table which holds the {@linkplain #getColumnName() indicator column}
	 */
	String getTableName();

	/**
	 * Create a SQL AST Assignment for setting the soft-delete column to its indicated "deleted" value
	 *
	 * @param tableReference Reference for the table containing the soft-delete column
	 */
	Assignment createSoftDeleteAssignment(TableReference tableReference);

	/**
	 * Create a SQL AST Predicate for restricting matches to non-deleted rows
	 *
	 * @param tableReference Reference for the table containing the soft-delete column
	 */
	Predicate createNonDeletedRestriction(TableReference tableReference);

	/**
	 * Create a SQL AST Predicate for restricting matches to non-deleted rows
	 *
	 * @param tableReference Reference for the table containing the soft-delete column
	 * @param expressionResolver Resolver for SQL AST Expressions
	 */
	Predicate createNonDeletedRestriction(TableReference tableReference, SqlExpressionResolver expressionResolver);

	/**
	 * Create a ColumnValueBinding for non-deleted indicator.
	 *
	 * @param softDeleteColumnReference Reference to the soft-delete column
	 *
	 * @apiNote Generally used as a restriction in a SQL AST
	 */
	ColumnValueBinding createNonDeletedValueBinding(ColumnReference softDeleteColumnReference);

	/**
	 * Create a ColumnValueBinding for deleted indicator.
	 *
	 * @param softDeleteColumnReference Reference to the soft-delete column
	 *
	 * @apiNote Generally used as an assignment in a SQL AST
	 */
	ColumnValueBinding createDeletedValueBinding(ColumnReference softDeleteColumnReference);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SelectableMapping

	@Override
	default String getSelectionExpression() {
		return getColumnName();
	}

	@Override
	default String getSelectableName() {
		return getColumnName();
	}

	@Override
	default String getContainingTableExpression() {
		return getTableName();
	}

	@Override
	default String getCustomReadExpression() {
		return null;
	}

	@Override
	default String getCustomWriteExpression() {
		return null;
	}

	@Override
	default boolean isFormula() {
		return false;
	}

	@Override
	default boolean isNullable() {
		return false;
	}

	@Override
	default boolean isInsertable() {
		return true;
	}

	@Override
	default boolean isUpdateable() {
		return true;
	}

	@Override
	default boolean isPartitioned() {
		return false;
	}

	@Override
	default String getColumnDefinition() {
		return null;
	}

	@Override
	default Long getLength() {
		return null;
	}

	@Override
	default Integer getPrecision() {
		return null;
	}

	@Override
	default Integer getScale() {
		return null;
	}

	@Override
	default Integer getTemporalPrecision() {
		return null;
	}
}
