/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
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

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSingularJoin(this);
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
	public SqmSingularJoin<O, T> copy(SqmCopyContext context) {
		final SqmSingularJoin<O, T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final SqmSingularJoin<O, T> path = context.registerCopy(
				this,
				new SqmSingularJoin<>(
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
	public SqmPathSource<T> getNodeType() {
		return getReferencedPathSource();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return getModel().getPathSource();
	}

	@Override
	public SingularPersistentAttribute<O, T> getModel() {
		return (SingularPersistentAttribute<O, T>) super.getNodeType();
	}

	@Override
	public SingularPersistentAttribute<O, T> getAttribute() {
		return getModel();
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedSingularJoin<O, T, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedSingularJoin<>( this, (TreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedSingularJoin<O, T, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSingularJoin<>( this, treatTarget, alias, fetch ) );
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
				getNavigablePath(),
				getReferencedPathSource().getPathName()
		);
	}

	@Override
	public SqmAttributeJoin<O, T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmSingularJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getAttribute(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
