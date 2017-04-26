/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableTypeBasic;

/**
 * @author Steve Ebersole
 */
public class LiteralFalseSqmExpression extends AbstractLiteralSqmExpressionImpl<Boolean> {
	public LiteralFalseSqmExpression(BasicValuedExpressableType expressionType) {
		super( Boolean.FALSE, expressionType );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralFalseExpression( this );
	}
}
