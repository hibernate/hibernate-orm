/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Locale;

/**
 * @author Gavin King
 */
public class SqmPowerFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "power";

	private final SqmExpression base;
	private final SqmExpression power;

	public SqmPowerFunction(
			SqmExpression<?> base,
			SqmExpression<?> power,
			AllowableFunctionReturnType<T> resultType,
			NodeBuilder nodeBuilder) {
		super( resultType, nodeBuilder );
		this.base = base;
		this.power = power;
	}

	public SqmExpression getBase() {
		return base;
	}

	public SqmExpression getPower() {
		return power;
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
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPowerFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s( %s, %s )",
				NAME,
				base.asLoggableText(),
				power.asLoggableText()
		);
	}
}
