/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmSubstringFunction extends AbstractSqmFunction {
	public static final String NAME = "substring";
	public static final String ALT_NAME = "substr";

	private final SqmExpression source;
	private final SqmExpression startPosition;
	private final SqmExpression length;

	public SqmSubstringFunction(
			BasicValuedExpressableType resultType,
			SqmExpression source,
			SqmExpression startPosition,
			SqmExpression length) {
		super( resultType );
		this.source = source;
		this.startPosition = startPosition;
		this.length = length;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	public SqmExpression getSource() {
		return source;
	}

	public SqmExpression getStartPosition() {
		return startPosition;
	}

	public SqmExpression getLength() {
		return length;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitSubstringFunction( this );
	}

	@Override
	public String asLoggableText() {
		StringBuilder buff = new StringBuilder( getFunctionName() )
				.append( '(' )
				.append( getSource().asLoggableText() );

		if ( getStartPosition() != null ) {
			buff.append( ", " ).append( getStartPosition().asLoggableText() );
		}

		if ( getLength() != null ) {
			buff.append( ", " ).append( getLength().asLoggableText() );
		}

		return buff.append( ")" ).toString();
	}
}
