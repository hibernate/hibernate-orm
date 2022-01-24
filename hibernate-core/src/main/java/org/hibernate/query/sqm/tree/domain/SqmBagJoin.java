/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Collection;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

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
	public BagPersistentAttribute<O,E> getReferencedPathSource() {
		return (BagPersistentAttribute<O,E>) super.getReferencedPathSource();
	}

	@Override
	public BagPersistentAttribute<O,E> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public BagPersistentAttribute<O,E> getAttribute() {
		//noinspection unchecked
		return (BagPersistentAttribute<O, E>) super.getAttribute();
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
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedBagJoin<O,E,S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedBagJoin<>( this, treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public SqmAttributeJoin<O, E> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmBagJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
