/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

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
			SqmExpressible<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, inherentType, nodeBuilder );
		this.name = name;
	}

	@Override
	public SqmNamedParameter<T> copy(SqmCopyContext context) {
		final SqmNamedParameter<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNamedParameter<T> expression = context.registerCopy(
				this,
				new SqmNamedParameter<>(
						name,
						allowMultiValuedBinding(),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String toString() {
		return "SqmNamedParameter(" + getName() + ")";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmNamedParameter<>( getName(), allowMultiValuedBinding(), this.getNodeType(), nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( ':' );
		sb.append( getName() );
	}

	@Override
	public int compareTo(SqmParameter anotherParameter) {
		return anotherParameter instanceof SqmNamedParameter<?>
				? getName().compareTo( ( (SqmNamedParameter<?>) anotherParameter ).getName() )
				: -1;
	}
}
