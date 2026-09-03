/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.translation;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.Stack;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.SetReturningFunctionType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Translates one [#getSqlAst SQL AST] and produces a [JdbcOperation].
///
/// Translators are created by a
/// [org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory] for a single
/// translation and are not reusable. Extend
/// [org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator] for custom SQL
/// rendering. Implement this contract directly, normally on top of
/// [org.hibernate.sql.ast.spi.AbstractSqlAstWalker], when the JDBC driver uses
/// a fundamentally different command language.
///
/// Direct implementations should create query operations with
/// [org.hibernate.sql.exec.spi.JdbcOperations]. A
/// [org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest.ModelMutation]
/// is not a query operation; create its result through
/// [org.hibernate.sql.ast.spi.model.TableMutation#createMutationOperation(String, List)].
///
/// @since 8.0
/// @author Steve Ebersole
/// @see org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory#buildTranslator
/// @see org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory#createTranslator
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface SqlAstTranslator<T extends JdbcOperation> extends SqlAstWalker {
	/// Perform this translator's one translation.
	///
	/// @param jdbcParameterBindings parameter values available for literalization
	/// and JDBC binding
	/// @param queryOptions execution options which affect translation
	/// @return the JDBC operation produced from this translator's SQL AST
	@SPI({ IMPLEMENT, SUPPLY })
	T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions);

	/// The SQL AST assigned to this translator.
	@Incubating
	Statement getSqlAst();

	/// The SessionFactory whose services are used during translation.
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
	@SPI(SPI.Role.USE)
	void renderNamedSetReturningFunction(String functionName, List<? extends SqlAstNode> sqlAstArguments, SetReturningFunctionType tupleType, String tableIdentifierVariable, SqlAstNodeRenderingMode argumentRenderingMode);

	/// Render the given SQL AST node using the requested expression mode and the
	/// current translator context.
	void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode);

	/// The query part at the top of the translator's traversal stack.
	QueryPart getCurrentQueryPart();

	/// Read-only access to the translator's current clause stack. Providers should
	/// inspect but not mutate this stack.
	Stack<Clause> getCurrentClauseStack();

	/**
	 * Not the best spot for this.  Returns the table names collected while walking the SQL AST.
	 * It's ok here because the translator is consider a one-time-use.  It just needs to be called
	 * after translation.
	 *
	 * A better option is probably to have "translation" objects that expose the affected table-names.
	 */
	Set<String> getAffectedTableNames();

	/// Register an affected physical table name while translating a mutation.
	void addAffectedTableName(String tableName);
}
