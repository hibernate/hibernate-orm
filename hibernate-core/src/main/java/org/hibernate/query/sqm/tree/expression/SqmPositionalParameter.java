/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Models a positional parameter expression
 *
 * @author Steve Ebersole
 */
public class SqmPositionalParameter extends AbstractSqmParameter {
	private final int position;

	public SqmPositionalParameter(int position, boolean canBeMultiValued) {
		this( position, canBeMultiValued, null );
	}

	public SqmPositionalParameter(int position, boolean canBeMultiValued, AllowableParameterType expressableType) {
		super( canBeMultiValued, expressableType );
		this.position = position;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public SqmParameter copy() {
		return new SqmPositionalParameter( getPosition(), allowMultiValuedBinding() );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPositionalParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "?" + getPosition();
	}

}
