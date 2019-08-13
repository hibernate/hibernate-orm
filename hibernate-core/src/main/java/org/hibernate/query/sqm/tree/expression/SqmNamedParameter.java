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
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter<T> extends AbstractSqmParameter<T> {
	private final String name;

	public SqmNamedParameter(String name, boolean canBeMultiValued, NodeBuilder nodeBuilder) {
		this( name, canBeMultiValued, null, nodeBuilder );
	}

	public SqmNamedParameter(
			String name,
			boolean canBeMultiValued,
			AllowableParameterType<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, inherentType, nodeBuilder );
		this.name = name;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public DomainResultProducer<T> getDomainResultProducer() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmNamedParameter<>( getName(), allowMultiValuedBinding(), this.getNodeType(), nodeBuilder() );
	}
}
