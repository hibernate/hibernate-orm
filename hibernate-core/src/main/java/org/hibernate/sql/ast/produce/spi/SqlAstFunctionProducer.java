/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Contract for functions impls that would like to control the
 * translation of the SQM expression into the SQL AST function
 * <p/>
 * {@link #convertToSqlAst} is the main contract here.  It will
 * be called as we walk the SQM tree to generate SQL AST and
 * asked to produce the equivalent SQL AST expression
 *
 * @author Steve Ebersole
 */
public interface SqlAstFunctionProducer extends SqmFunction {
	/**
	 * Generate the SQL AST form of the function as an expression.
	 *
	 * To be able to use this in the select-clause, the returned
	 * expression must also implement the
	 * {@link DomainResultProducer}
	 * contract
	 */
	Expression convertToSqlAst(SqmToSqlAstConverter walker);

	@Override
	default <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSqlAstFunctionProducer( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// these are not needed since we perform the production of the SQL AST directly

	@Override
	default String asLoggableText() {
		return null;
	}

	@Override
	default String getFunctionName() {
		return null;
	}

	@Override
	default AllowableFunctionReturnType getExpressableType() {
		return null;
	}

	@Override
	default ExpressableType getInferableType() {
		return null;
	}

	@Override
	default boolean hasArguments() {
		return false;
	}
}
