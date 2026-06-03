/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedCrossJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicateCollection;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;

/**
 * Stuff and things
 *
 * @implNote IMPL NOTE
 *
 * @author Steve Ebersole
 */
public class SqmCrossJoin<L, T> extends AbstractSqmFrom<L, T> implements JpaCrossJoin<L, T>, SqmJoin<L, T> {
	private final SqmRoot<L> sqmRoot;
	private final SqmPredicateCollection sqmJoinPredicates;

	public SqmCrossJoin(
			SqmEntityDomainType<T> joinedEntityDescriptor,
			@Nullable String alias,
			SqmRoot<L> sqmRoot) {
		this(
				buildRootNavigablePath( joinedEntityDescriptor.getHibernateEntityName(), alias ),
				joinedEntityDescriptor,
				alias,
				sqmRoot
		);
	}

	protected SqmCrossJoin(
			NavigablePath navigablePath,
			SqmEntityDomainType<T> joinedEntityDescriptor,
			@Nullable String alias,
			SqmRoot<L> sqmRoot) {
		super(
				navigablePath,
				joinedEntityDescriptor,
				sqmRoot,
				alias,
				sqmRoot.nodeBuilder()
		);
		this.sqmRoot = sqmRoot;
		this.sqmJoinPredicates = new SqmWhereClause( sqmRoot.nodeBuilder() );
	}

	@Override
	public boolean isImplicitlySelectable() {
		return true;
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public SqmJoinType getSqmJoinType() {
		return SqmJoinType.CROSS;
	}

	@Override
	public SqmCrossJoin<L, T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmCrossJoin<>(
						getNavigablePath(),
						getReferencedPathSource(),
						getExplicitAlias(),
						getRoot().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmRoot<L> getRoot() {
		return sqmRoot;
	}

	@Nullable
	@Override
	public SqmFrom<?, T> getLhs() {
		// a cross-join has no LHS
		return null;
	}

	@Override
	public SqmEntityDomainType<T> getReferencedPathSource() {
		return (SqmEntityDomainType<T>) super.getReferencedPathSource();
	}

	public String getEntityName() {
		return getReferencedPathSource().getHibernateEntityName();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCrossJoin( this );
	}

	@Override
	@Nonnull
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	@Nonnull
	public SqmCorrelatedCrossJoin<L, T> createCorrelation() {
		return new SqmCorrelatedCrossJoin<>( this );
	}

	public SqmCrossJoin<L, T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmCrossJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				creationProcessingState.getPathRegistry()
						.resolveFromByPath( getRoot().getNavigablePath() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nullable
	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		return null;
	}

	@Override
	public @Nullable SqmPredicate getJoinPredicate() {
		return sqmJoinPredicates.getPredicate();
	}

	@Override
	public void setJoinPredicate(@Nullable SqmPredicate predicate) {
		sqmJoinPredicates.setPredicate( predicate );
	}

	@Override
	@Nonnull
	public SqmCrossJoin<L, T> on(@Nonnull BooleanExpression... restrictions) {
		sqmJoinPredicates.setPredicate( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	@Override
	@Nonnull
	public From<?, L> getParent() {
		@SuppressWarnings("unchecked")
		final var parent = (From<?, L>) getLhs();
		return castNonNull( parent );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias, false );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		if ( alias != null ) {
			throw new IllegalArgumentException( "Cross join treats can not be aliased" );
		}
		if ( fetch ) {
			throw new IllegalArgumentException( "Cross join treats can not be fetched" );
		}
		final var treat = (SqmTreatedCrossJoin<L, T, S>) findTreat( treatTarget, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedCrossJoin<>( this, (SqmEntityDomainType<S>) treatTarget ) );
		}
		else {
			return treat;
		}
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull Class<S> treatAsType) {
		return treatAs( treatAsType, null, false );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedCrossJoin<L, T, S> treatAs(@Nonnull EntityDomainType<S> treatAsType) {
		return treatAs( treatAsType, null, false );
	}

}
