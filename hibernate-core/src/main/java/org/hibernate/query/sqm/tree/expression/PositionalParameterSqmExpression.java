/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class PositionalParameterSqmExpression implements ParameterSqmExpression {
	private final int position;
	private final boolean canBeMultiValued;
	private ExpressableType expressableType;

	public PositionalParameterSqmExpression(int position, boolean canBeMultiValued) {
		this.position = position;
		this.canBeMultiValued = canBeMultiValued;
	}

	public PositionalParameterSqmExpression(int position, boolean canBeMultiValued, ExpressableType expressableType) {
		this.position = position;
		this.canBeMultiValued = canBeMultiValued;
		this.expressableType = expressableType;
	}

	@Override
	public ExpressableType getExpressionType() {
		return expressableType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPositionalParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "?" + getPosition();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public void impliedType(ExpressableType expressableType) {
		if ( expressableType != null ) {
			this.expressableType = expressableType;
		}
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	@Override
	public ExpressableType getAnticipatedType() {
		return getExpressionType();
	}
}
