/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;


import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.model.ast.ColumnValueBinding;

/**
 * Metadata about temporal columns for entities enabled for temporal history.
 *
 * @see org.hibernate.annotations.Temporal
 */
public interface TemporalMapping {
	String ROLE_NAME = "{temporal}";

	String getStartingColumnName();

	String getEndingColumnName();

	String getTableName();

	SelectableMapping getStartingColumnMapping();

	SelectableMapping getEndingColumnMapping();

	JdbcMapping getJdbcMapping();

	Predicate createCurrentRestriction(TableReference tableReference);

	Predicate createCurrentRestriction(TableReference tableReference, SqlExpressionResolver expressionResolver);

	Predicate createRestriction(TableReference tableReference, Object temporalValue);

	Predicate createRestriction(TableReference tableReference, SqlExpressionResolver expressionResolver, Object temporalValue);

	ColumnValueBinding createStartingValueBinding(ColumnReference startingColumnReference);

	ColumnValueBinding createEndingValueBinding(ColumnReference endingColumnReference);

	ColumnValueBinding createNullEndingValueBinding(ColumnReference endingColumnReference);
}
