/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Set;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmSetJoin<O, E>
		extends AbstractSqmPluralJoin<O,Set<E>, E>
		implements JpaSetJoin<O, E> {
	public SqmSetJoin(
			SqmFrom<?,O> lhs,
			SetPersistentAttribute<O, E> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmSetJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SetPersistentAttribute<O, E> pluralValuedNavigable,
			String alias, SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, pluralValuedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmSetJoin<O, E> copy(SqmCopyContext context) {
		final SqmSetJoin<O, E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final SqmSetJoin<O, E> path = context.registerCopy(
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
	public SetPersistentAttribute<O, E> getModel() {
		return (SetPersistentAttribute<O, E>) super.getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSetJoin( this );
	}

	@Override
	public SetPersistentAttribute<O, E> getAttribute() {
		return getModel();
	}

	@Override
	public SqmSetJoin<O, E> on(JpaExpression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(Expression<Boolean> restriction) {
		return (SqmSetJoin<O, E>) super.on( restriction );
	}

	@Override
	public SqmSetJoin<O, E> on(JpaPredicate... restrictions) {
		return (SqmSetJoin<O, E>) super.on( restrictions );
	}

	@Override
	public SqmSetJoin<O, E> on(Predicate... restrictions) {
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
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O, E, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedSetJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedSetJoin<>( this, (TreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends E> SqmTreatedSetJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedSetJoin<O, E, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSetJoin<>( this, treatTarget, alias, fetch ) );
		}
		return treat;
	}

	@Override
	public <X, Y> SqmAttributeJoin<X, Y> fetch(String attributeName) {
		return fetch( attributeName, JoinType.INNER);
	}


	@Override
	public SqmAttributeJoin<O, E> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmSetJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getModel(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
