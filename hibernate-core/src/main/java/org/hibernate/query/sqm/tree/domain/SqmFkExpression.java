/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.spi.NavigablePath;

/**
 * Reference to the key-side (as opposed to the target-side) of the
 * foreign-key of a to-one association.
 *
 * @author Steve Ebersole
 */
public class SqmFkExpression<T> extends AbstractSqmPath<T> {

	/**
	 * @deprecated Use {@link #SqmFkExpression(SqmEntityValuedSimplePath)} instead.
	 */
	@Deprecated(forRemoval = true)
	public SqmFkExpression(SqmEntityValuedSimplePath<?> toOnePath, NodeBuilder criteriaBuilder) {
		this( toOnePath );
	}

	public SqmFkExpression(SqmEntityValuedSimplePath<?> toOnePath) {
		this( toOnePath.getNavigablePath().append( ForeignKeyDescriptor.PART_NAME ), toOnePath );
	}

	@SuppressWarnings("unchecked")
	private SqmFkExpression(
			NavigablePath navigablePath,
			SqmEntityValuedSimplePath<?> toOnePath) {
		super(
				navigablePath,
				(SqmPathSource<T>) pathDomainType( toOnePath ).getIdentifierDescriptor(),
				toOnePath,
				toOnePath.nodeBuilder()
		);
	}

	private static IdentifiableDomainType<?> pathDomainType(SqmEntityValuedSimplePath<?> toOnePath) {
		return (IdentifiableDomainType<?>) toOnePath.getNodeType();
	}

	public SqmEntityValuedSimplePath<?> getToOnePath() {
		return (SqmEntityValuedSimplePath<?>) getLhs();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFkExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "fk(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}

	@Override
	public SqmFkExpression<T> copy(SqmCopyContext context) {
		final SqmFkExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEntityValuedSimplePath<?> lhsCopy = (SqmEntityValuedSimplePath<?>) getLhs().copy( context );
		return context.registerCopy(
				this,
				new SqmFkExpression<T>( getNavigablePathCopy( lhsCopy ), lhsCopy )
		);
	}

	@Override
	public <S extends T> SqmPath<S> treatAs(Class<S> treatJavaType) {
		throw new FunctionArgumentException( "Fk paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmPath<S> treatAs(EntityDomainType<S> treatTarget) {
		throw new FunctionArgumentException( "Fk paths cannot be TREAT-ed" );
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}
}
