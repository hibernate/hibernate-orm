/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
public class SqmPluralPartJoin<O,T> extends AbstractSqmJoin<O,T> {

	public SqmPluralPartJoin(
			SqmFrom<?,O> lhs,
			SqmPathSource<T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super(
				SqmCreationHelper.buildSubNavigablePath( lhs, joinedNavigable.getPathName(), alias ),
				joinedNavigable,
				lhs,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				nodeBuilder
		);
	}

	protected SqmPluralPartJoin(
			SqmFrom<?, O> lhs,
			NavigablePath navigablePath,
			SqmPathSource<T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super(
				navigablePath,
				joinedNavigable,
				lhs,
				alias == SqmCreationHelper.IMPLICIT_ALIAS ? null : alias,
				joinType,
				nodeBuilder
		);
	}

	@Override
	public boolean isImplicitlySelectable() {
		return false;
	}

	@Override
	public SqmPluralPartJoin<O, T> copy(SqmCopyContext context) {
		final SqmPluralPartJoin<O, T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFrom<?, O> lhsCopy = getLhs().copy( context );
		final SqmPluralPartJoin<O, T> path = context.registerCopy(
				this,
				new SqmPluralPartJoin<>(
						lhsCopy,
						getNavigablePathCopy( lhsCopy ),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return null;
	}

	@Override
	public void setJoinPredicate(SqmPredicate predicate) {
		throw new UnsupportedOperationException( "Setting a predicate for a plural part join is unsupported" );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitPluralPartJoin( this );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType, String alias) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget, String alias) {
		final SqmTreatedPluralPartJoin treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedPluralPartJoin( this, (SqmEntityDomainType<?>) treatTarget, alias ) );
		}
		return treat;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias, fetch );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedPluralPartJoin treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		final SqmTreatedPluralPartJoin treat = findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedPluralPartJoin( this, (SqmEntityDomainType<?>) treatTarget, alias ) );
		}
		return treat;
	}


	@Override
	public PersistentAttribute<? super O, ?> getAttribute() {
		return null;
	}

	@Override
	public SqmCorrelatedPluralPartJoin<O, T> createCorrelation() {
		return new SqmCorrelatedPluralPartJoin<>( this );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmPluralPartJoin(%s : %s)",
				getNavigablePath(),
				getReferencedPathSource().getPathName()
		);
	}
}
