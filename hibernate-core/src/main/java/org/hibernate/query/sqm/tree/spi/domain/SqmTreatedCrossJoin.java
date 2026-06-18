/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.from.SqmCrossJoin;
import org.hibernate.spi.NavigablePath;

/**
 * A TREAT form of {@linkplain SqmCrossJoin}
 *
 * @author Steve Ebersole
 */
public class SqmTreatedCrossJoin<L, T, S extends T>
		extends SqmCrossJoin<L, S>
		implements SqmTreatedJoin<L, T, S> {
	private final SqmCrossJoin<L, T> wrappedPath;
	private final SqmEntityDomainType<S> treatTarget;

	public SqmTreatedCrossJoin(
			SqmCrossJoin<L, T> wrappedPath,
			SqmEntityDomainType<S> treatTarget) {
		super(
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getHibernateEntityName(), null ),
				treatTarget,
				null,
				wrappedPath.getRoot()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedCrossJoin(
			NavigablePath navigablePath,
			SqmCrossJoin<L, T> wrappedPath,
			SqmEntityDomainType<S> treatTarget) {
		super(
				navigablePath,
				treatTarget,
				null,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	@Nonnull
	public SqmTreatedCrossJoin<L, T, S> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmTreatedCrossJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public void setExplicitAlias(@Nullable String explicitAlias) {
		throw new UnsupportedOperationException("Treated cross joins doesn't support explicit alias");
	}

	@Nonnull
	@Override
	public SqmEntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Nonnull
	@Override
	public SqmEntityDomainType<S> getModel() {
		return treatTarget;
	}

	@Override
	public SqmCrossJoin<L, T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public @Nonnull SqmBindableType<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
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
	@Nonnull
	public SqmTreatedCrossJoin<L, T, S> on(@Nullable JpaPredicate ... restrictions) {
		return (SqmTreatedCrossJoin<L, T, S>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmTreatedCrossJoin<L, T, S> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmTreatedCrossJoin<L, T, S>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmTreatedCrossJoin<L, T, S> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmTreatedCrossJoin<L, T, S>) super.on( restriction );
	}

	@Override
	@Nonnull
	public <S1 extends S> SqmTreatedCrossJoin<L, S, S1> treatAs(
			@Nonnull EntityDomainType<S1> treatTarget,
			@Nullable String alias,
			boolean fetch) {
		@SuppressWarnings("unchecked")
		final var treat = (SqmTreatedCrossJoin<L, S, S1>)
				wrappedPath.treatAs( treatTarget, alias, fetch );
		return treat;
	}
}
