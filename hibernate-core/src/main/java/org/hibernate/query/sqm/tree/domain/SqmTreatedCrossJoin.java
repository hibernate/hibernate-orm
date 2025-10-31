/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.spi.NavigablePath;

/**
 * A TREAT form of {@linkplain SqmCrossJoin}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class SqmTreatedCrossJoin extends SqmCrossJoin implements SqmTreatedJoin {
	private final SqmCrossJoin wrappedPath;
	private final SqmEntityDomainType treatTarget;

	public SqmTreatedCrossJoin(
			SqmCrossJoin<?> wrappedPath,
			SqmEntityDomainType<?> treatTarget) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getHibernateEntityName(), null ),
				(SqmEntityDomainType) wrappedPath.getReferencedPathSource().getPathType(),
				null,
				wrappedPath.getRoot()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedCrossJoin(
			NavigablePath navigablePath,
			SqmCrossJoin<?> wrappedPath,
			SqmEntityDomainType<?> treatTarget) {
		//noinspection unchecked
		super(
				navigablePath,
				(SqmEntityDomainType) wrappedPath.getReferencedPathSource().getPathType(),
				null,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedCrossJoin copy(SqmCopyContext context) {
		final SqmTreatedCrossJoin existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedCrossJoin path = context.registerCopy(
				this,
				new SqmTreatedCrossJoin(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget
				)
		);
		//noinspection unchecked
		copyTo( path, context );
		return path;
	}

	@Override
	public void setExplicitAlias(@Nullable String explicitAlias) {
		throw new UnsupportedOperationException("Treated cross joins doesn't support explicit alias");
	}

	@Override
	public SqmEntityDomainType getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmEntityDomainType getModel() {
		return treatTarget;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SqmPath getWrappedPath() {
		return wrappedPath;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public @NonNull SqmBindableType getNodeType() {
		return treatTarget;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public SqmEntityDomainType getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
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
	public SqmTreatedCrossJoin on(Predicate @Nullable ... restrictions) {
		return (SqmTreatedCrossJoin) super.on( restrictions );
	}

	@Override
	public SqmTreatedCrossJoin on(JpaPredicate @Nullable ... restrictions) {
		return (SqmTreatedCrossJoin) super.on( restrictions );
	}

	@Override
	public SqmTreatedCrossJoin on(@Nullable Expression restriction) {
		//noinspection unchecked
		return (SqmTreatedCrossJoin) super.on( restriction );
	}

	@Override
	public SqmTreatedCrossJoin on(@Nullable JpaExpression restriction) {
		//noinspection unchecked
		return (SqmTreatedCrossJoin) super.on( restriction );
	}

	@Override
	public SqmTreatedCrossJoin treatAs(EntityDomainType treatTarget, @Nullable String alias, boolean fetch) {
		//noinspection unchecked
		return wrappedPath.treatAs( treatTarget, alias, fetch );
	}
}
