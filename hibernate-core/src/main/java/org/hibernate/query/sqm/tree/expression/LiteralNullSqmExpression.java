/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeBasic;
import org.hibernate.query.sqm.domain.type.SqmDomainType;
import org.hibernate.query.sqm.domain.SqmExpressableType;

/**
 * @author Steve Ebersole
 */
public class LiteralNullSqmExpression implements LiteralSqmExpression<Void> {
	private SqmExpressableType injectedExpressionType;

	public LiteralNullSqmExpression() {
		injectedExpressionType = NULL_TYPE;
	}

	@Override
	public Void getLiteralValue() {
		return null;
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return injectedExpressionType;
	}

	@Override
	public SqmExpressableType getInferableType() {
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

	private static SqmDomainTypeBasic NULL_TYPE = new SqmDomainTypeBasic() {
		@Override
		public SqmDomainTypeBasic getExportedDomainType() {
			return null;
		}

		@Override
		public Class getJavaType() {
			return void.class;
		}

		@Override
		public String asLoggableText() {
			return "NULL";
		}
	};

	@Override
	public SqmDomainType getExportedDomainType() {
		return injectedExpressionType.getExportedDomainType();
	}

	@Override
	public void impliedType(SqmExpressableType type) {
		injectedExpressionType = type;
	}
}
