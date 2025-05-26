/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.EntityType;

import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin<L,R>
		extends AbstractSqmJoin<L,R>
		implements SqmSingularValuedJoin<L,R>, JpaEntityJoin<L,R> {
	private final SqmRoot<L> sqmRoot;

	public SqmEntityJoin(
			EntityDomainType<R> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot<L> sqmRoot) {
		this(
				SqmCreationHelper.buildRootNavigablePath( joinedEntityDescriptor.getHibernateEntityName(), alias ),
				joinedEntityDescriptor,
				alias,
				joinType,
				sqmRoot
		);
	}

	protected SqmEntityJoin(
			NavigablePath navigablePath,
			EntityDomainType<R> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot<L> sqmRoot) {
		super( navigablePath, (SqmEntityDomainType<R>) joinedEntityDescriptor, sqmRoot, alias, joinType, sqmRoot.nodeBuilder() );
		this.sqmRoot = sqmRoot;
	}

	public SqmEntityJoin(
			EntityType<R> entity,
			String alias,
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
		final SqmEntityJoin<L,R> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEntityJoin<L,R> path = context.registerCopy(
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
	public SqmFrom<?, L> getParent() {
		return getRoot();
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}

	@Override
	public SqmEntityDomainType<R> getModel() {
		return (SqmEntityDomainType<R>) super.getModel();
	}

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
	public SqmEntityJoin<L,R> on(JpaExpression<Boolean> restriction) {
		return (SqmEntityJoin<L,R>) super.on( restriction );
	}

	@Override
	public SqmEntityJoin<L,R> on(Expression<Boolean> restriction) {
		return (SqmEntityJoin<L,R>) super.on( restriction );
	}

	@Override
	public SqmEntityJoin<L,R> on(JpaPredicate... restrictions) {
		return (SqmEntityJoin<L,R>) super.on( restrictions );
	}

	@Override
	public SqmEntityJoin<L,R> on(Predicate... restrictions) {
		return (SqmEntityJoin<L,R>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedEntityJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(Class<S> treatAsType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatAsType ) );
	}

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(EntityDomainType<S> treatAsType) {
		final SqmTreatedEntityJoin<L,R,S> treat = findTreat( treatAsType, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedEntityJoin<>( this, (SqmEntityDomainType<S>) treatAsType, null ) );
		}
		else {
			return treat;
		}
	}

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(Class<S> treatJavaType, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public <S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		// there is no attribute
		return null;
	}

	@Override
	public SqmCorrelatedEntityJoin<L,R> createCorrelation() {
		return new SqmCorrelatedEntityJoin<>( this );
	}

	public SqmEntityJoin<L,R> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmEntityJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				pathRegistry.findFromByPath( getRoot().getNavigablePath() )
		);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SqmEntityJoin<?, ?> that
			&& Objects.equals( this.getEntityName(), that.getEntityName() )
			&& super.equals( other );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), getEntityName() );
	}
}
