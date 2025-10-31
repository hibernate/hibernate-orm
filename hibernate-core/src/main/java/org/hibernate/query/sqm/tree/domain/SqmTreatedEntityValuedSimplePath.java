/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityValuedSimplePath<T, S extends T>
		extends SqmEntityValuedSimplePath<S>
		implements SqmSimplePath<S>, SqmTreatedPath<T,S> {

	private final SqmEntityDomainType<S> treatTarget;
	private final SqmPath<T> wrappedPath;

	public SqmTreatedEntityValuedSimplePath(
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

	public SqmTreatedEntityValuedSimplePath(
			SqmPath<T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedEntityValuedSimplePath(
			NavigablePath navigablePath,
			SqmPath<T> wrappedPath,
			SqmEntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmPathSource<S>) wrappedPath.getReferencedPathSource(),
				castNonNull( wrappedPath.getLhs() ),
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
	public SqmEntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @NonNull SqmEntityDomainType<S> getNodeType() {
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
	public <S1 extends S> SqmTreatedEntityValuedSimplePath<S,S1> treatAs(Class<S1> treatJavaType) {
		return super.treatAs( treatJavaType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<?> get(String attributeName) {
		return resolvePath( attributeName, treatTarget.getSubPathSource( attributeName ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		// Cast needed for Checker Framework
		return walker.visitTreatedPath( (SqmTreatedPath<?, @Nullable ?>) this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getName() );
		hql.append( ')' );
	}
}
