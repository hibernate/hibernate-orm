/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * Models a positional parameter expression
 *
 * @author Steve Ebersole
 */
public class SqmPositionalParameter<T> extends AbstractSqmParameter<T> {
	private final int position;

	public SqmPositionalParameter(
			int position,
			boolean canBeMultiValued,
			NodeBuilder nodeBuilder) {
		this( position, canBeMultiValued, null, nodeBuilder );
	}

	public SqmPositionalParameter(
			int position,
			boolean canBeMultiValued,
			AllowableParameterType<T> expressableType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, expressableType, nodeBuilder );
		this.position = position;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmPositionalParameter<>( getPosition(), allowMultiValuedBinding(), this.getNodeType(), nodeBuilder() );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitPositionalParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "?" + getPosition();
	}

}
