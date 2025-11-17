/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PathSource;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmListJoin<O,E>
		extends AbstractSqmPluralJoin<O,List<E>, E>
		implements JpaListJoin<O, E> {
	public SqmListJoin(
			SqmFrom<?,O> lhs,
			SqmListPersistentAttribute<? super O, E> listAttribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, listAttribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmListJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SqmListPersistentAttribute<O, E> listAttribute,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, listAttribute, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmListJoin<O, E> copy(SqmCopyContext context) {
		final SqmListJoin<O, E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final SqmListJoin<O, E> path = context.registerCopy(
				this,
				new SqmListJoin<>(
						lhsCopy,
						getNavigablePathCopy( lhsCopy ),
						getAttribute(),
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
	public SqmListPersistentAttribute<O, E> getModel() {
		return (SqmListPersistentAttribute<O, E>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitListJoin( this );
	}

	@Override
	public SqmListPersistentAttribute<O,E> getAttribute() {
		return getModel();
	}

	@Override
	public SqmPath<Integer> index() {
		final PathSource<Integer> indexPathSource = getAttribute().getIndexPathSource();
		return resolvePath( indexPathSource.getPathName(), (SqmPathSource<Integer>) indexPathSource );
	}

	@Override
	public SqmListJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmListJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmListJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmListJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmListJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmListJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmListJoin<O, E> on(Predicate... restrictions) {
		return (SqmListJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedListJoin<O, E> createCorrelation() {
		return new SqmCorrelatedListJoin<>( this );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O, E, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedListJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<S> ) {
				return addTreat( new SqmTreatedListJoin<>( this, (SqmTreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedListJoin<O,E,S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedListJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
		}
		return treat;
	}

}
