/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.function.FunctionSqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

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
public interface SqlAstFunctionProducer extends FunctionSqmExpression {
	Expression convertToSqlAst(SqmToSqlAstConverter walker);

	@Override
	default <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSqlAstFunctionProducer( this );
	}

	@Override
	default String asLoggableText() {
		return null;
	}

	@Override
	default String getFunctionName() {
		return null;
	}

	@Override
	default AllowableFunctionReturnType getExpressionType() {
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
