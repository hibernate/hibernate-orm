/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmCoalesceFunction extends AbstractSqmFunction {
	public static final String NAME = "coalesce";

	private List<SqmExpression> arguments = new ArrayList<>();

	public SqmCoalesceFunction() {
		this( null );
	}

	public SqmCoalesceFunction(AllowableFunctionReturnType resultType) {
		this( resultType, null );
	}

	public SqmCoalesceFunction(
			AllowableFunctionReturnType resultType, List<SqmExpression> arguments) {
		super( resultType );
		this.arguments = arguments;
	}

	public List<SqmExpression> getArguments() {
		return arguments;
	}

	public void value(SqmExpression expression) {
		arguments.add( expression );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCoalesceFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
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
