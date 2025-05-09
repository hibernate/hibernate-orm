/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
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
public class SqmTreatedMapJoin<L, K, V, S extends V> extends SqmMapJoin<L, K, S> implements SqmTreatedAttributeJoin<L,V,S> {
	private final SqmMapJoin<L, K, V> wrappedPath;
	private final SqmTreatableDomainType<S> treatTarget;

	public SqmTreatedMapJoin(
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedMapJoin(
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				( (SqmMapJoin<L, K, S>) wrappedPath ).getModel(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedMapJoin(
			NavigablePath navigablePath,
			SqmMapJoin<L, K, V> wrappedPath,
			SqmTreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				( (SqmMapJoin<L, K, S>) wrappedPath ).getModel(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> copy(SqmCopyContext context) {
		final SqmTreatedMapJoin<L, K, V, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedMapJoin<L, K, V, S> path = context.registerCopy(
				this,
				new SqmTreatedMapJoin<>(
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
	public SqmMapJoin<L,K,V> getWrappedPath() {
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
	public SqmTreatableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(Class<S1> treatJavaType) {
		return super.treatAs( treatJavaType );
	}

	@Override
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(EntityDomainType<S1> treatTarget) {
		return super.treatAs( treatTarget );
	}

	@Override
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(Class<S1> treatJavaType, String alias) {
		return super.treatAs( treatJavaType, alias );
	}

	@Override
	public <S1 extends S> SqmTreatedMapJoin<L, K, S, S1> treatAs(EntityDomainType<S1> treatTarget, String alias) {
		return super.treatAs( treatTarget, alias );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(JpaExpression<Boolean> restriction) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(Expression<Boolean> restriction) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(JpaPredicate... restrictions) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedMapJoin<L, K, V, S> on(Predicate... restrictions) {
		return (SqmTreatedMapJoin<L, K, V, S>) super.on( restrictions );
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
		return object instanceof SqmTreatedMapJoin<?, ?, ?, ?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.treatTarget.getTypeName(), that.treatTarget.getTypeName() )
			&& Objects.equals( this.wrappedPath.getNavigablePath(), that.wrappedPath.getNavigablePath() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( treatTarget.getTypeName(), wrappedPath.getNavigablePath() );
	}
}
