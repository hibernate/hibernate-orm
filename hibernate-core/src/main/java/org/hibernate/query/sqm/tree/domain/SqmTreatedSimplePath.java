/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSimplePath<T, S extends T>
		extends SqmEntityValuedSimplePath<S>
		implements SqmSimplePath<S>, SqmTreatedPath<T,S> {

	private final SqmEntityDomainType<S> treatTarget;
	private final SqmPath<T> wrappedPath;

	public SqmTreatedSimplePath(
			SqmPluralValuedSimplePath<T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
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

	public SqmTreatedSimplePath(
			SqmPath<T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
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

	private SqmTreatedSimplePath(
			NavigablePath navigablePath,
			SqmPath<T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
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
	public SqmTreatedSimplePath<T, S> copy(SqmCopyContext context) {
		final SqmTreatedSimplePath<T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmTreatedSimplePath<T, S> path = context.registerCopy(
				this,
				new SqmTreatedSimplePath<>(
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
	public SqmEntityDomainType<S> getTreatTarget() {
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
	public SqmPathSource<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public <S1 extends S> SqmTreatedEntityValuedSimplePath<S,S1> treatAs(Class<S1> treatJavaType)
			throws PathException {
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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTreatedSimplePath<?, ?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getName(), that.treatTarget.getName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getName(), wrappedPath.getNavigablePath() );
	}
}
