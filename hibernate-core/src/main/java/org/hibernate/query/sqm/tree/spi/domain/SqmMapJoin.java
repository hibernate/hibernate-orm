/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PathSource;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.spi.NavigablePath;

import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<L, K, V>
		extends AbstractSqmPluralJoin<L, Map<K, V>, V>
		implements JpaMapJoin<L, K, V> {
	public SqmMapJoin(
			SqmFrom<?, L> lhs,
			SqmMapPersistentAttribute<? super L, K, V> pluralValuedNavigable,
			@Nullable String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmMapJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			SqmMapPersistentAttribute<L, K, V> pluralValuedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, pluralValuedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmMapJoin<L, K, V> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, L> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
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

	@Nonnull
	@Override
	public SqmMapPersistentAttribute<L, K, V> getModel() {
		return (SqmMapPersistentAttribute<L, K, V>) super.getModel();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMapJoin( this );
	}

	@Override
	public @Nonnull SqmMapPersistentAttribute<L, K, V> getAttribute() {
		return getModel();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmPath<K> key() {
		final PathSource<K> keyPathSource = getAttribute().getKeyPathSource();
		return resolvePath( keyPathSource.getPathName(), (SqmPathSource<K>) keyPathSource );
	}

	@Nonnull
	@Override
	public SqmPath<V> value() {
		final var elementPathSource = getAttribute().getElementPathSource();
		return resolvePath( elementPathSource.getPathName(), elementPathSource );
	}

	@Nonnull
	@Override
	public Expression<Map.Entry<K, V>> entry() {
		return new SqmMapEntryReference<>( this, nodeBuilder() );
	}

	@Override
	@Nonnull
	public SqmMapJoin<L, K, V> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmMapJoin<L, K, V>) super.on( restriction );
	}

	@Nonnull
	@Override
	public SqmMapJoin<L, K, V> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmMapJoin<L, K, V>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmMapJoin<L, K, V> on(@Nullable JpaPredicate... restrictions) {
		return (SqmMapJoin<L, K, V>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmMapJoin<L, K, V> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmMapJoin<L, K, V>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmMapJoin<L, K, V> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return (SqmMapJoin<L, K, V>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmCorrelatedMapJoin<L, K, V> createCorrelation() {
		return new SqmCorrelatedMapJoin<>( this );
	}

	@Nonnull
	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Nonnull
	@Override
	public <S extends V> SqmMapJoin<L, K, S> treat(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType );
	}

	@Override
	@Nonnull
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	@Nonnull
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		final var treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final var treat = (SqmTreatedMapJoin<L, K, V, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<S> ) {
				return addTreat( new SqmTreatedMapJoin<>( this, (SqmTreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		else {
			return treat;
		}
	}

	@Nonnull
	@Override
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	@Nonnull
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final var treat = (SqmTreatedMapJoin<L, K, V, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedMapJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
		}
		else {
			return treat;
		}
	}

	@Override
	@Nonnull
	public <S extends V> SqmTreatedMapJoin<L, K, V, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		final var treat = (SqmTreatedMapJoin<L, K, V, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedMapJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias ) );
		}
		else {
			return treat;
		}
	}

}
