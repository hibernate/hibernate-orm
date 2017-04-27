/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class LiteralNullSqmExpression implements LiteralSqmExpression<Void> {
	private ExpressableType injectedExpressionType;

	public LiteralNullSqmExpression() {
		injectedExpressionType = NULL_TYPE;
	}

	@Override
	public Void getLiteralValue() {
		return null;
	}

	@Override
	public ExpressableType getExpressionType() {
		return injectedExpressionType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralNullExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	private static BasicValuedExpressableType NULL_TYPE = new BasicValuedExpressableType() {
		@Override
		public String getTypeName() {
			return "<null-type>";
		}

		@Override
		public PersistenceType getPersistenceType() {
			return null;
		}

		@Override
		public Class getJavaType() {
			return void.class;
		}
	};

	@Override
	public void impliedType(ExpressableType type) {
		injectedExpressionType = type;
	}
}
