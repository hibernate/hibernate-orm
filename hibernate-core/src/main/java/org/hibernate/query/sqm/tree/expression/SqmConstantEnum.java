/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmConstantEnum<T extends Enum> extends AbstractSqmLiteral<T> implements SqmConstantReference<T> {
	public SqmConstantEnum(T value, BasicValuedExpressableType inherentType) {
		super( value, inherentType );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitConstantEnumExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "EnumConstant(" + getLiteralValue() + ")";
	}
}
