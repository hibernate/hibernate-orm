/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class SqmNullifFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "nullif";

	private final SqmExpression<T> first;
	private final SqmExpression<T> second;

	public SqmNullifFunction(
			SqmExpression<T> first,
			SqmExpression<T> second,
			NodeBuilder nodeBuilder) {
		this(
				first,
				second,
				(AllowableFunctionReturnType<T>) coalesce( first.getExpressableType(), second.getExpressableType() ),
				nodeBuilder
		);
	}

	public SqmNullifFunction(
			SqmExpression<T> first,
			SqmExpression<T> second,
			AllowableFunctionReturnType<T> resultType,
			NodeBuilder nodeBuilder) {
		super( resultType, nodeBuilder );
		this.first = first;
		this.second = second;
	}

	public SqmExpression getFirstArgument() {
		return first;
	}

	public SqmExpression getSecondArgument() {
		return second;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNullifFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s( %s, %s )",
				NAME,
				getFirstArgument().asLoggableText(),
				getSecondArgument().asLoggableText()
		);
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}
}
