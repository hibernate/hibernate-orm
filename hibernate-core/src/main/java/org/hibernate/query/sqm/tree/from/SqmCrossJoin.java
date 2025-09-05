/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedFrom;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicateCollection;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;


import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;

/**
 * Stuff and things
 *
 * @apiNote {@linkplain SqmCrossJoin} and its offspring are largely de-typed to account
 *         for {@linkplain SqmCrossJoin} having only one type argument for the right-hand
 *         side.  To properly handle the type parameters in the hierarchy we would need to
 *         change this to accept type parameter for the left-handle side as well.
 *         <p/>
 *         Another option is to not make it a join as in `SqmJoin`.  Instead, model it
 *         as a root with its predicate(s) added to an internal `SqmPredicateCollection` (ansi join predicate)
 *         or to the query where clause (theta joins).
 *
 * @implNote IMPL NOTE
 *
 * @author Steve Ebersole
 */
public class SqmCrossJoin<T> extends AbstractSqmFrom<T, T> implements JpaCrossJoin<T>, SqmJoin<T, T> {
	private final SqmRoot<?> sqmRoot;
	private final SqmPredicateCollection sqmJoinPredicates;

	public SqmCrossJoin(
			SqmEntityDomainType<T> joinedEntityDescriptor,
			String alias,
			SqmRoot<?> sqmRoot) {
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
			String alias,
			SqmRoot<?> sqmRoot) {
		super(
				navigablePath,
				joinedEntityDescriptor,
				sqmRoot,
				alias,
				sqmRoot.nodeBuilder()
		);
		this.sqmRoot = sqmRoot;
		this.sqmJoinPredicates = new SqmWhereClause( nodeBuilder() );
	}

	@Override
	public boolean isImplicitlySelectable() {
		return true;
	}

	@Override
	public JoinType getJoinType() {
		return null;
	}

	@Override
	public SqmJoinType getSqmJoinType() {
		return SqmJoinType.CROSS;
	}

	@Override
	public SqmCrossJoin<T> copy(SqmCopyContext context) {
		final SqmCrossJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCrossJoin<T> path = context.registerCopy(
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

	public SqmRoot<?> getRoot() {
		return sqmRoot;
	}

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
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	public SqmCorrelatedCrossJoin<T> createCorrelation() {
		return new SqmCorrelatedCrossJoin<>( this );
	}

	public SqmCrossJoin<T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCrossJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				pathRegistry.findFromByPath( getRoot().getNavigablePath() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA



	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		return null;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return sqmJoinPredicates.getPredicate();
	}

	@Override
	public void setJoinPredicate(SqmPredicate predicate) {
		sqmJoinPredicates.setPredicate( predicate );
	}

	@Override
	public From<?, T> getParent() {
		return getLhs();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedCrossJoin treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Cross join treats can not be aliased" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedCrossJoin treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Cross join treats can not be aliased" );
	}

	@Override
	public <S extends T> SqmTreatedFrom<T, T, S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		return null;
	}

	@Override
	public <S extends T> SqmTreatedFrom<T, T, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedCrossJoin treatAs(Class<S> treatAsType) {
		return treatAs( treatAsType, null );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> SqmTreatedCrossJoin treatAs(EntityDomainType<S> treatAsType) {
		return treatAs( treatAsType, null );
	}

}
