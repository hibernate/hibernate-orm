/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralInteger extends AbstractSqmLiteral<Integer> {
	public SqmLiteralInteger(Integer value, BasicValuedExpressableType sqmExpressableTypeBasic) {
		super( value, sqmExpressableTypeBasic );
	}

	public SqmLiteralInteger(int i) {
		this( i, StandardSpiBasicTypes.INTEGER );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralIntegerExpression( this );
	}
}
