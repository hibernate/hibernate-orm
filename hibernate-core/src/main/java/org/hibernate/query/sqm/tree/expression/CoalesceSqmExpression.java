/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.type.SqmDomainType;
import org.hibernate.query.sqm.domain.SqmExpressableType;

/**
 * @author Steve Ebersole
 */
public class CoalesceSqmExpression implements SqmExpression {
	private List<SqmExpression> values = new ArrayList<>();

	public List<SqmExpression> getValues() {
		return values;
	}

	public void value(SqmExpression expression) {
		values.add( expression );
	}

	@Override
	public SqmExpressableType getExpressionType() {
		return values.get( 0 ).getExpressionType();
	}

	@Override
	public SqmExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public SqmDomainType getExportedDomainType() {
		return getExpressionType().getExportedDomainType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCoalesceExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<coalesce>";
	}
}
