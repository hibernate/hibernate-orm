/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Map;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<L, K, V>
		extends AbstractSqmPluralJoin<L, Map<K, V>, V>
		implements JpaMapJoin<L, K, V> {
	public SqmMapJoin(
			SqmFrom<?, L> lhs,
			MapPersistentAttribute<L, K, V> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmMapJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			MapPersistentAttribute<L, K, V> pluralValuedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, pluralValuedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmMapJoin<L, K, V> copy(SqmCopyContext context) {
		final SqmMapJoin<L, K, V> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, L> lhsCopy = getLhs().copy( context );
		final SqmMapJoin<L, K, V> path = context.registerCopy(
				this,
				new SqmMapJoin<>(
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
	public MapPersistentAttribute<L, K, V> getModel() {
		return (MapPersistentAttribute<L, K, V>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMapJoin( this );
	}

	@Override
	public MapPersistentAttribute<L, K, V> getAttribute() {
		return getModel();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmPath<K> key() {
		final SqmPathSource<K> keyPathSource = getAttribute().getKeyPathSource();
		return resolvePath( keyPathSource.getPathName(), keyPathSource );
	}

	@Override
	public SqmPath<V> value() {
		final SqmPathSource<V> elementPathSource = getAttribute().getElementPathSource();
		return resolvePath( elementPathSource.getPathName(), elementPathSource );
	}

	@Override
	public Expression<Map.Entry<K, V>> entry() {
		return new SqmMapEntryReference<>( this, nodeBuilder() );
	}

	@Override
	public SqmMapJoin<L, K, V> on(JpaExpression<Boolean> restriction) {
		return (SqmMapJoin<L, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<L, K, V> on(Expression<Boolean> restriction) {
		return (SqmMapJoin<L, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<L, K, V> on(JpaPredicate... restrictions) {
		return (SqmMapJoin<L, K, V>) super.on( restrictions );
	}

	@Override
	public SqmMapJoin<L, K, V> on(Predicate... restrictions) {
		return (SqmMapJoin<L, K, V>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedMapJoin<L, K, V> createCorrelation() {
		return new SqmCorrelatedMapJoin<>( this );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedMapJoin<L, K, V, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedMapJoin<>( this, (TreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedMapJoin<L, K, V, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedMapJoin<>( this, treatTarget, alias, fetch ) );
		}
		return treat;
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedMapJoin<L, K, V, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedMapJoin<>( this, treatTarget, alias ) );
		}
		return treat;
	}

	@Override
	public SqmMapJoin<L, K, V> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmMapJoin<>(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getAttribute(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}

}
