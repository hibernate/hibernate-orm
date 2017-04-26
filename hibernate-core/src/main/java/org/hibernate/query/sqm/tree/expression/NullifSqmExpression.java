/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.type.SqmDomainType;
import org.hibernate.query.sqm.domain.SqmExpressableType;

/**
 * @author Steve Ebersole
 */
public class NullifSqmExpression implements SqmExpression {
	private final SqmExpression first;
	private final SqmExpression second;

	public NullifSqmExpression(SqmExpression first, SqmExpression second) {
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
	public SqmExpressableType getExpressionType() {
		return first.getExpressionType();
	}

	@Override
	public SqmExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitNullifExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "NULLIF(" + first.asLoggableText() + ", " + second.asLoggableText() + ")";
	}

	@Override
	public SqmDomainType getExportedDomainType() {
		return first.getExportedDomainType();
	}
}
