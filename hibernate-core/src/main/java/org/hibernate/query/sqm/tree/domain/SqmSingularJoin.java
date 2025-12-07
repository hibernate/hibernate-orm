/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularJoin<O,T> extends AbstractSqmAttributeJoin<O,T> implements SqmSingularValuedJoin<O,T> {

	private final SqmSingularPersistentAttribute<? super O, T> attribute;

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
		attribute = joinedNavigable;
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
		attribute = joinedNavigable;
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
	public @NonNull SqmBindableType<T> getNodeType() {
		return getReferencedPathSource().getExpressible();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return getModel().getSqmPathSource();
	}

	@Override
	public SqmSingularPersistentAttribute<? super O, T> getModel() {
		return attribute;
	}

	@Override
	public @NonNull SqmSingularPersistentAttribute<? super O, T> getAttribute() {
		return getModel();
	}

	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	public <S extends T> SqmTreatedAttributeJoin<O, T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		final ManagedDomainType<S> treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final SqmTreatedSingularJoin<O, T, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof SqmTreatableDomainType<S> treatableDomainType ) {
				return addTreat( new SqmTreatedSingularJoin<>( this, treatableDomainType, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final SqmTreatedSingularJoin<O, T, S> treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedSingularJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
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

}
