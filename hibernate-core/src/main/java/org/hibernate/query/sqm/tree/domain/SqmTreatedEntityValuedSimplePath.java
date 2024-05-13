/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityValuedSimplePath<T, S extends T>
		extends SqmEntityValuedSimplePath<S>
		implements SqmSimplePath<S>, SqmTreatedPath<T,S> {

	private final EntityDomainType<S> treatTarget;
	private final SqmPath<T> wrappedPath;

	public SqmTreatedEntityValuedSimplePath(
			SqmPluralValuedSimplePath<T> wrappedPath,
			EntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	public SqmTreatedEntityValuedSimplePath(
			SqmPath<T> wrappedPath,
			EntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedEntityValuedSimplePath(
			NavigablePath navigablePath,
			SqmPath<T> wrappedPath,
			EntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedEntityValuedSimplePath<T, S> copy(SqmCopyContext context) {
		final SqmTreatedEntityValuedSimplePath<T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmTreatedEntityValuedSimplePath<T, S> path = context.registerCopy(
				this,
				new SqmTreatedEntityValuedSimplePath<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						getTreatTarget(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityDomainType<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public <S1 extends S> SqmTreatedEntityValuedSimplePath<S,S1> treatAs(Class<S1> treatJavaType) throws PathException {
		return super.treatAs( treatJavaType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<?> get(String attributeName) {
		return resolvePath( attributeName, treatTarget.getSubPathSource( attributeName ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
		sb.append( ')' );
	}
}
