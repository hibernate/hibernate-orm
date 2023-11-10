/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
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
			ListPersistentAttribute<O, E> listAttribute,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, listAttribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmListJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			ListPersistentAttribute<O, E> listAttribute,
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
	public ListPersistentAttribute<O, E> getModel() {
		return (ListPersistentAttribute<O, E>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitListJoin( this );
	}

	@Override
	public ListPersistentAttribute<O,E> getAttribute() {
		return getModel();
	}

	@Override
	public SqmPath<Integer> index() {
		final SqmPathSource<Integer> indexPathSource = getAttribute().getIndexPathSource();
		return resolvePath( indexPathSource.getPathName(), indexPathSource );
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
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ), null );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@Override
	public <S extends E> SqmTreatedListJoin<O,E,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedListJoin<O,E,S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedListJoin<>( this, treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public SqmAttributeJoin<O, E> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmListJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getAttribute(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}

}
