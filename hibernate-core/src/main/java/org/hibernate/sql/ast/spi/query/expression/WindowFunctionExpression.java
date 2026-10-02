package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nullable;

import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Models a window function expression at the SQL AST level.
 *
 * @author Christian Beikov
 */
public interface WindowFunctionExpression extends FunctionExpression {

	Predicate getFilter();

	@Nullable
	Boolean getRespectNulls();

	@Nullable
	Boolean getFromFirst();
}
