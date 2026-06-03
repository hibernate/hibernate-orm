/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.EntityType;

import java.util.List;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;


/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin<L,R>
		extends AbstractSqmJoin<L,R>
		implements SqmSingularValuedJoin<L,R>, JpaEntityJoin<L,R> {
	private final SqmRoot<L> sqmRoot;

	public SqmEntityJoin(
			EntityDomainType<R> joinedEntityDescriptor,
			@Nullable String alias,
			SqmJoinType joinType,
			SqmRoot<L> sqmRoot) {
		this(
				buildRootNavigablePath( joinedEntityDescriptor.getHibernateEntityName(), alias ),
				joinedEntityDescriptor,
				alias,
				joinType,
				sqmRoot
		);
	}

	protected SqmEntityJoin(
			NavigablePath navigablePath,
			EntityDomainType<R> joinedEntityDescriptor,
			@Nullable String alias,
			SqmJoinType joinType,
			SqmRoot<L> sqmRoot) {
		super( navigablePath,
				(SqmEntityDomainType<R>) joinedEntityDescriptor,
				sqmRoot, alias, joinType, sqmRoot.nodeBuilder() );
		this.sqmRoot = sqmRoot;
	}

	public SqmEntityJoin(
			EntityType<R> entity,
			@Nullable String alias,
			JoinType joinType,
			SqmRoot<L> root) {
		this( (EntityDomainType<R>) entity, alias, SqmJoinType.from( joinType ), root );
	}

	@Override
	public boolean isImplicitlySelectable() {
		return true;
	}

	@Override
	public SqmEntityJoin<L,R> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmEntityJoin<>(
						getNavigablePath(),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						getRoot().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmRoot<L> getRoot() {
		return sqmRoot;
	}

	@Override
	@Nonnull
	public SqmFrom<?, L> getParent() {
		return getRoot();
	}

	@Override
	@Nonnull
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	@Nonnull
	public SqmEntityDomainType<R> getModel() {
		return (SqmEntityDomainType<R>) super.getModel();
	}

	@Nullable
	@Override
	public SqmFrom<?,L> getLhs() {
		// An entity-join has no LHS
		return null;
	}

	@Override
	public SqmEntityDomainType<R> getReferencedPathSource() {
		return (SqmEntityDomainType<R>) super.getReferencedPathSource();
	}

	public String getEntityName() {
		return getReferencedPathSource().getHibernateEntityName();
	}

	@Override
	@Nonnull
	public SqmEntityJoin<L,R> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmEntityJoin<L,R>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmEntityJoin<L,R> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmEntityJoin<L,R>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmEntityJoin<L,R> on(@Nullable JpaPredicate... restrictions) {
		return (SqmEntityJoin<L,R>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmEntityJoin<L, R> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmEntityJoin<L,R>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmEntityJoin<L, R> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return (SqmEntityJoin<L,R>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedEntityJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Nonnull
	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatAsType) {
		final var treat = (SqmTreatedEntityJoin<L, R, S>) findTreat( treatAsType, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedEntityJoin<>( this, (SqmEntityDomainType<S>) treatAsType, null ) );
		}
		else {
			return treat;
		}
	}

	@Override
	@Nonnull
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	@Nonnull
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	@Nonnull
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	@Nonnull
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Nullable
	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		// there is no attribute
		return null;
	}

	@Override
	@Nonnull
	public SqmCorrelatedEntityJoin<L,R> createCorrelation() {
		return new SqmCorrelatedEntityJoin<>( this );
	}

	public SqmEntityJoin<L,R> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final var pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmEntityJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				pathRegistry.resolveFromByPath( getRoot().getNavigablePath() )
		);
	}
}
