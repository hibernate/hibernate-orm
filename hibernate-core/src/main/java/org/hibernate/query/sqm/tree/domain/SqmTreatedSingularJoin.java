/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSingularJoin<O,T, S extends T>
		extends SqmSingularJoin<O,S>
		implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmSingularJoin<O,T> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedSingularJoin(
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSingularJoin(
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.treatAs( treatTarget.getTypeName(), alias ),
				(SqmSingularPersistentAttribute<O, S>)
						wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSingularJoin(
			NavigablePath navigablePath,
			SqmSingularJoin<O,T> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SqmSingularPersistentAttribute<O, S>)
						wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedSingularJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedSingularJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedSingularJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedSingularJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias(),
						isFetched()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSingularJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public TreatableDomainType<S> getTreatTarget() {
		return treatTarget;
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
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "treat(" );
		wrappedPath.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( treatTarget.getTypeName() );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmTreatedSingularJoin<?, ?, ?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getTypeName(), that.treatTarget.getTypeName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getTypeName(), wrappedPath.getNavigablePath() );
	}

	@Override
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(Class<S1> treatJavaType) {
		return (SqmTreatedSingularJoin<O, S, S1>) super.treatAs( treatJavaType );
	}

	@Override
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(EntityDomainType<S1> treatTarget) {
		return (SqmTreatedSingularJoin<O, S, S1>) super.treatAs( treatTarget );
	}

	@Override
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(Class<S1> treatJavaType, String alias) {
		return (SqmTreatedSingularJoin<O, S, S1>) super.treatAs( treatJavaType, alias );
	}

	@Override
	public <S1 extends S> SqmTreatedSingularJoin<O, S, S1> treatAs(EntityDomainType<S1> treatTarget, String alias) {
		return (SqmTreatedSingularJoin<O, S, S1>) super.treatAs( treatTarget, alias );
	}

	@Override
	public SqmTreatedSingularJoin<O,T,S> on(JpaExpression<Boolean> restriction) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSingularJoin<O,T,S> on(JpaPredicate... restrictions) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedSingularJoin<O,T,S> on(Expression<Boolean> restriction) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSingularJoin<O,T,S> on(Predicate... restrictions) {
		return (SqmTreatedSingularJoin<O, T, S>) super.on( restrictions );
	}
}
