/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmNullifFunction extends AbstractSqmFunction {
	public static final String NAME = "nullif";

	private final SqmExpression first;
	private final SqmExpression second;

	public SqmNullifFunction(
			SqmExpression first,
			SqmExpression second) {
		super( (AllowableFunctionReturnType) (first == null ? second.getExpressableType() : first.getExpressableType()) );
		this.first = first;
		this.second = second;
	}

	public SqmNullifFunction(
			SqmExpression first,
			SqmExpression second,
			AllowableFunctionReturnType resultType) {
		super( resultType );
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
	public <T> T accept(SemanticQueryWalker<T> walker) {
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
