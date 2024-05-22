/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEmbeddedValuedSimplePath<T, S extends T> extends SqmEmbeddedValuedSimplePath<S>
		implements SqmTreatedPath<T, S> {
	private final SqmPath<T> wrappedPath;
	private final EmbeddableDomainType<S> treatTarget;

	@SuppressWarnings( { "unchecked" } )
	public SqmTreatedEmbeddedValuedSimplePath(
			SqmPath<T> wrappedPath,
			EmbeddableDomainType<S> treatTarget) {
		super(
				wrappedPath.getNavigablePath().treatAs( treatTarget.getTypeName() ),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@SuppressWarnings( { "unchecked" } )
	private SqmTreatedEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPath<T> wrappedPath,
			EmbeddableDomainType<S> treatTarget) {
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedEmbeddedValuedSimplePath<T, S> copy(SqmCopyContext context) {
		final SqmTreatedEmbeddedValuedSimplePath<T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedEmbeddedValuedSimplePath<T, S> path = context.registerCopy(
				this,
				new SqmTreatedEmbeddedValuedSimplePath<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EmbeddableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public EmbeddableDomainType<S> getReferencedPathSource() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<?> getLhs() {
		return wrappedPath.getLhs();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getTypeName() );
		sb.append( ')' );
	}
}
