/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.List;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Differs from {@link SqmConcat} in that
 * the function can have multiple arguments, whereas ConcatExpression only has 2.
 *
 * CONCAT is also one of Hibernate's standard SQM functions which every Dialect
 * must support.
 *
 * @see SqmConcat
 *
 * @author Steve Ebersole
 */
public class SqmConcatFunction extends AbstractSqmFunction {
	public static final String NAME = "concat";

	private final List<SqmExpression> expressions;

	public SqmConcatFunction(
			BasicValuedExpressableType resultType,
			List<SqmExpression> expressions) {
		super( resultType );
		this.expressions = expressions;

		assert expressions != null;
		assert expressions.size() >= 2;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	public List<SqmExpression> getExpressions() {
		return expressions;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitConcatFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "CONCAT(...)";
	}
}
