/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public interface SqlAstTranslator<T extends JdbcOperation> extends SqlAstWalker {

	SessionFactoryImplementor getSessionFactory();

	/**
	 * Returns the literal value of the given expression, inlining a parameter value if necessary.
	 * @since 7.0
	 */
	<X> X getLiteralValue(Expression expression);

	/**
	 * Renders the given SQL AST node with the given rendering mode.
	 */
	void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode);

	/**
	 * Whether the FILTER clause for aggregate functions is supported.
	 */
	boolean supportsFilterClause();

	/**
	 * Returns the current query part that is translated.
	 */
	QueryPart getCurrentQueryPart();

	Stack<Clause> getCurrentClauseStack();

	/**
	 * Not the best spot for this.  Its the table names collected while walking the SQL AST.
	 * Its ok here because the translator is consider a one-time-use.  It just needs to be called
	 * after translation.
	 *
	 * A better option is probably to have "translation" objects that expose the affected table-names.
	 */
	Set<String> getAffectedTableNames();

	void addAffectedTableName(String tableName);

	T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);
}
