/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmSetJoin<O, E>
		extends AbstractSqmPluralJoin<O,Set<E>, E>
		implements JpaSetJoin<O, E> {
	public SqmSetJoin(
			SqmFrom<?,O> lhs,
			SqmSetPersistentAttribute<? super O, E> pluralValuedNavigable,
			@Nullable String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmSetJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SqmSetPersistentAttribute<O, E> pluralValuedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, pluralValuedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmSetJoin<O, E> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmSetJoin<>(
						lhsCopy,
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						getExplicitAlias(),
						getSqmJoinType(),
						context.copyFetchedFlag() && isFetched(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSetPersistentAttribute<O, E> getModel() {
//		return (SqmSetPersistentAttribute<O, E>) super.getNodeType();
		return (SqmSetPersistentAttribute<O, E>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSetJoin( this );
	}

	@Override
	public @NonNull SqmSetPersistentAttribute<O, E> getAttribute() {
		return getModel();
	}

	@Override
	public SqmSetJoin<O, E> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(@Nullable Expression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(JpaPredicate @Nullable... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmSetJoin<O, E> on(Predicate @Nullable... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedSetJoin<O, E> createCorrelation() {
		return new SqmCorrelatedSetJoin<>( this );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O, E, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedSetJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedSetJoin<>( this, (SqmTreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final SqmTreatedSetJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSetJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
		}
		return treat;
	}
}
