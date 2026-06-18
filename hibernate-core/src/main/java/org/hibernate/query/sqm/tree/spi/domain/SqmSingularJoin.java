/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import java.util.Locale;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmTreatedAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularJoin<O,T> extends AbstractSqmAttributeJoin<O,T> implements SqmSingularValuedJoin<O,T> {

	public SqmSingularJoin(
			SqmFrom<?,O> lhs,
			SqmSingularPersistentAttribute<? super O, T> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super(
				lhs,
				joinedNavigable.createNavigablePath( lhs, alias ),
				joinedNavigable,
				alias,
				joinType,
				fetched,
				nodeBuilder
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSingularJoin(this);
	}

	protected SqmSingularJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SqmSingularPersistentAttribute<? super O, T> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmSingularJoin<O, T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
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
	public @Nonnull SqmBindableType<T> getNodeType() {
		return getReferencedPathSource().getExpressible();
	}

	@Nonnull
	@Override
	public SqmSingularPersistentAttribute<? super O, T> getModel() {
		return (SqmSingularPersistentAttribute<? super O, T>) super.getModel();
	}

	@Override
	public @Nonnull SqmSingularPersistentAttribute<? super O, T> getAttribute() {
		return getModel();
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		final var treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final var treat = (SqmTreatedSingularJoin<O, T, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof SqmTreatableDomainType<S> treatableDomainType ) {
				return addTreat( new SqmTreatedSingularJoin<>( this, treatableDomainType, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		else {
			return treat;
		}
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final var treat = (SqmTreatedSingularJoin<O, T, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSingularJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
		}
		else {
			return treat;
		}
	}

	@Override
	@Nonnull
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

}
