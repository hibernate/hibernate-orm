/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmJoin<L, R> extends AbstractSqmFrom<L, R> implements SqmJoin<L, R> {
	private final SqmJoinType joinType;
	private SqmPredicate onClausePredicate;

	public AbstractSqmJoin(
			NavigablePath navigablePath,
			SqmPathSource<R> referencedNavigable,
			SqmFrom<?, L> lhs,
			String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, alias, nodeBuilder );
		this.joinType = joinType;
	}

	@Override
	public SqmJoinType getSqmJoinType() {
		return joinType;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}

	@Override
	public void setJoinPredicate(SqmPredicate predicate) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Setting join predicate [%s] (was [%s])",
					predicate.toString(),
					this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
			);
		}

		this.onClausePredicate = predicate;
	}

	public void applyRestriction(SqmPredicate restriction) {
		if ( this.onClausePredicate == null ) {
			this.onClausePredicate = restriction;
		}
		else {
			this.onClausePredicate = nodeBuilder().and( onClausePredicate, restriction );
		}
	}

	protected void copyTo(AbstractSqmJoin<L, R> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.onClausePredicate = onClausePredicate == null ? null : onClausePredicate.copy( context );
	}

	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, String alias);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(Class<S> treatJavaType, String alias, boolean fetched);

	@Override
	public abstract <S extends R> SqmTreatedJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetched);

	@Override
	public SqmFrom<?, L> getLhs() {
		//noinspection unchecked
		return (SqmFrom<?, L>) super.getLhs();
	}

	@Override
	public SqmFrom<?, L> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return joinType.getCorrespondingJpaJoinType();
	}
	@Override
	public SqmPredicate getOn() {
		return getJoinPredicate();
	}

	@Override
	public SqmJoin<L, R> on(JpaExpression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmJoin<L, R> on(Expression<Boolean> restriction) {
		applyRestriction( nodeBuilder().wrap( restriction ) );
		return this;
	}

	@Override
	public SqmJoin<L, R> on(JpaPredicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public SqmJoin<L, R> on(Predicate... restrictions) {
		applyRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	public <X> SqmEntityJoin<R, X> join(Class<X> targetEntityClass) {
		return super.join( targetEntityClass, joinType );
	}

	@Override
	public <X> SqmEntityJoin<R, X> join(Class<X> targetEntityClass, SqmJoinType joinType) {
		return super.join( targetEntityClass, joinType );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof AbstractSqmJoin<?, ?> that
			&& super.equals( object )
			&& this.joinType == that.joinType; // unnecessary, but harmless
//			&& Objects.equals( onClausePredicate, that.onClausePredicate ); // including this causes problems
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), joinType );
	}
}
