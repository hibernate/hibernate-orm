/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
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
	 * Renders a named set returning function.
	 *
	 * @since 7.0
	 */
	@Incubating
	void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode);

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
	 * Not the best spot for this.  Returns the table names collected while walking the SQL AST.
	 * It's ok here because the translator is consider a one-time-use.  It just needs to be called
	 * after translation.
	 *
	 * A better option is probably to have "translation" objects that expose the affected table-names.
	 */
	Set<String> getAffectedTableNames();

	T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);
}
