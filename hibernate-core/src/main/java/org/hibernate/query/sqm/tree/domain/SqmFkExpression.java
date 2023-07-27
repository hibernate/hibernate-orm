/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Reference to the key-side (as opposed to the target-side) of the
 * foreign-key of a to-one association.
 *
 * @author Steve Ebersole
 */
public class SqmFkExpression<T> extends AbstractSqmExpression<T> {
	private final SqmEntityValuedSimplePath<?> toOnePath;

	@SuppressWarnings("unchecked")
	public SqmFkExpression(SqmEntityValuedSimplePath<?> toOnePath, NodeBuilder criteriaBuilder) {
		super( (SqmExpressible<? super T>) pathDomainType( toOnePath ).getIdType(), criteriaBuilder );
		this.toOnePath = toOnePath;
	}

	private static IdentifiableDomainType<?> pathDomainType(SqmEntityValuedSimplePath<?> toOnePath) {
		return (IdentifiableDomainType<?>) toOnePath.getNodeType();
	}

	public SqmEntityValuedSimplePath<?> getToOnePath() {
		return toOnePath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFkExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "fk(" );
		toOnePath.appendHqlString( sb );
		sb.append( ')' );
	}

	@Override
	public SqmExpression<T> copy(SqmCopyContext context) {
		final SqmFkExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		return context.registerCopy(
				this,
				new SqmFkExpression<T>( toOnePath.copy( context ), nodeBuilder() )
		);
	}
}
