/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmSingularJoin<O,T> extends AbstractSqmAttributeJoin<O,T> {
	public SqmSingularJoin(
			SqmFrom<?,O> lhs,
			SingularPersistentAttribute<O, T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	public SqmSingularJoin(
			SqmFrom<?,O> lhs,
			SqmJoinable joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	protected SqmSingularJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SingularPersistentAttribute<O, T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SingularPersistentAttribute<O, T> getReferencedPathSource() {
		return (SingularPersistentAttribute<O, T>) super.getReferencedPathSource();
	}

	@Override
	public SingularPersistentAttribute<O, T> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public SingularPersistentAttribute<O, T> getAttribute() {
		//noinspection unchecked
		return (SingularPersistentAttribute<O, T>) super.getAttribute();
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedSingularJoin<O, T, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSingularJoin<>( this, treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public SqmCorrelatedSingularJoin<O, T> createCorrelation() {
		return new SqmCorrelatedSingularJoin<>( this );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmSingularJoin(%s : %s)",
				getNavigablePath().getFullPath(),
				getReferencedPathSource().getPathName()
		);
	}

	@Override
	public SqmAttributeJoin<O, T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmSingularJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
