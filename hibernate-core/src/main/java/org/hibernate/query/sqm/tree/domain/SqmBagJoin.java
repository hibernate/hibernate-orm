/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Collection;

import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmBagJoin<O, E> extends AbstractSqmPluralJoin<O,Collection<E>, E> implements JpaCollectionJoin<O, E> {
	public SqmBagJoin(
			SqmFrom<?,O> lhs,
			BagPersistentAttribute<O,E> attribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, attribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmBagJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			BagPersistentAttribute<O,E> attribute,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, attribute, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmBagJoin<O, E> copy(SqmCopyContext context) {
		final SqmBagJoin<O, E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final SqmBagJoin<O, E> path = context.registerCopy(
				this,
				new SqmBagJoin<>(
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
	public BagPersistentAttribute<O,E> getModel() {
		return (BagPersistentAttribute<O, E>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBagJoin( this );
	}

	@Override
	public BagPersistentAttribute<O,E> getAttribute() {
		return getModel();
	}

	@Override
	public SqmBagJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmBagJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmBagJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmBagJoin<O, E> on(Predicate... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	// todo (6.0) : need to resolve these fetches against the element/index descriptors

	@Override
	public SqmCorrelatedBagJoin<O, E> createCorrelation() {
		return new SqmCorrelatedBagJoin<>( this );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedBagJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedBagJoin<>( this, (TreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedBagJoin<O,E,S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedBagJoin<>( this, treatTarget, alias, fetch ) );
		}
		return treat;
	}

	@Override
	public SqmAttributeJoin<O, E> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmBagJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getAttribute(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
