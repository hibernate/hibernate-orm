/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedRoot;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom<E,E> implements JpaRoot<E> {

	private final boolean allowJoins;
	private List<SqmJoin<?, ?>> orderedJoins;

	public SqmRoot(
			EntityDomainType<E> entityType,
			String alias,
			boolean allowJoins,
			NodeBuilder nodeBuilder) {
		super( entityType, alias, nodeBuilder );
		this.allowJoins = allowJoins;
	}

	protected SqmRoot(
			NavigablePath navigablePath,
			SqmPathSource<E> referencedNavigable,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
		this.allowJoins = true;
	}

	public SqmRoot(
			NavigablePath navigablePath,
			EntityDomainType<E> entityType,
			String alias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, (SqmEntityDomainType<E>) entityType, alias, nodeBuilder );
		this.allowJoins = true;
	}

	protected SqmRoot(
			NavigablePath navigablePath,
			SqmPathSource<E> referencedNavigable,
			String alias,
			boolean allowJoins,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, alias, nodeBuilder );
		this.allowJoins = allowJoins;
	}

	@Override
	public SqmRoot<E> copy(SqmCopyContext context) {
		final SqmRoot<E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmRoot<E> path = context.registerCopy(
				this,
				new SqmRoot<>(
						getNavigablePath(),
						getReferencedPathSource(),
						getExplicitAlias(),
						allowJoins,
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Internal
	public void copyTo(SqmRoot<E> target, SqmCopyContext context) {
		super.copyTo( target, context );
		if ( orderedJoins != null ) {
			target.orderedJoins = new ArrayList<>( orderedJoins.size() );
			for ( SqmJoin<?, ?> orderedJoin : orderedJoins ) {
				target.orderedJoins.add( orderedJoin.copy( context ) );
			}
		}
	}

	@Override
	public SqmPath<?> getLhs() {
		// a root has no LHS
		return null;
	}

	public boolean isAllowJoins() {
		return allowJoins;
	}

	public List<SqmJoin<?, ?>> getOrderedJoins() {
		return orderedJoins;
	}

	public void addOrderedJoin(SqmJoin<?, ?> join) {
		if ( orderedJoins == null ) {
			// If we encounter anything but an attribute join, we need to order joins strictly
			if ( !( join instanceof SqmAttributeJoin<?, ?> ) ) {
				orderedJoins = new ArrayList<>();
				visitSqmJoins( this::addOrderedJoinTransitive );
			}
		}
		else {
			orderedJoins.add( join );
		}
	}

	private void addOrderedJoinTransitive(SqmJoin<?, ?> join) {
		orderedJoins.add( join );
		join.visitSqmJoins( this::addOrderedJoinTransitive );
	}

	@Override
	public void addSqmJoin(SqmJoin<E, ?> join) {
		if ( !allowJoins ) {
			throw new IllegalArgumentException(
					"The root node [" + this + "] does not allow join/fetch"
			);
		}
		super.addSqmJoin( join );
	}

	@Override
	public SqmRoot<?> findRoot() {
		return this;
	}

	public String getEntityName() {
		return getModel().getHibernateEntityName();
	}

	@Override
	public String toString() {
		return getExplicitAlias() == null
				? getEntityName()
				: getEntityName() + " as " + getExplicitAlias();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootPath( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmEntityDomainType<E> getModel() {
		return (SqmEntityDomainType<E>) getReferencedPathSource();
	}

	@Override
	public EntityDomainType<E> getManagedType() {
		return getModel();
	}

	@Override
	public SqmCorrelatedRoot<E> createCorrelation() {
		return new SqmCorrelatedRoot<>( this );
	}

	public boolean containsOnlyInnerJoins() {
		for ( SqmJoin<E, ?> sqmJoin : getSqmJoins() ) {
			if ( sqmJoin.getSqmJoinType() != SqmJoinType.INNER ) {
				return false;
			}
		}
		return !hasTreats();
	}

	@Override
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		final SqmTreatedFrom<E,E,S> treat = findTreat( treatTarget, null );
		if ( treat == null ) {
			//noinspection rawtypes,unchecked
			return addTreat( (SqmTreatedFrom) new SqmTreatedRoot( this, (SqmEntityDomainType) treatTarget ) );
		}
		return treat;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends E> SqmTreatedRoot treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Root treats can not be aliased" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends E> SqmTreatedRoot treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Root treats can not be aliased" );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends E> SqmTreatedRoot treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		throw new TreatException( "Root paths cannot be aliased, nor fetched - " + getNavigablePath().getFullPath() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends E> SqmTreatedRoot treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		throw new TreatException( "Root paths cannot be aliased, nor fetched - " + getNavigablePath().getFullPath() );
	}

}
