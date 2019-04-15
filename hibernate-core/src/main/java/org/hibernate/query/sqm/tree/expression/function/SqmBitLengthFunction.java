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
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class SqmBitLengthFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "bit_length";

	private final SqmExpression argument;

	public SqmBitLengthFunction(SqmExpression<T> argument, NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this(
				argument,
				(AllowableFunctionReturnType<T>) StandardSpiBasicTypes.LONG,
				nodeBuilder
		);
	}

	public SqmBitLengthFunction(
			SqmExpression argument,
			AllowableFunctionReturnType<T> resultType,
			NodeBuilder nodeBuilder) {
		super( resultType, nodeBuilder );
		this.argument = argument;
	}

	public SqmExpression getArgument() {
		return argument;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBitLengthFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s( %s )",
				NAME,
				argument.asLoggableText()
		);
	}
}
