/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedRoot;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom<E,E> implements JpaRoot<E>, DomainResultProducer<E> {

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
		super( navigablePath, entityType, alias, nodeBuilder );
		this.allowJoins = true;
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

	@Override
	public EntityDomainType<E> getReferencedPathSource() {
		return (EntityDomainType<E>) super.getReferencedPathSource();
	}

	public String getEntityName() {
		return getReferencedPathSource().getHibernateEntityName();
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
	public EntityDomainType<E> getManagedType() {
		return getReferencedPathSource();
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
	public EntityDomainType<E> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public <S extends E> SqmTreatedRoot<E, S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends E> SqmTreatedRoot<E, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		final SqmTreatedRoot<E, S> treat = findTreat( treatTarget, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedRoot<>( this, treatTarget ) );
		}
		return treat;
	}

	@Override
	public <S extends E> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends E> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DomainResult<E> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final String entityName = getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getDomainModel()
				.getEntityDescriptor( entityName );
		return entityDescriptor.createDomainResult(
				getNavigablePath(),
				creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( getNavigablePath() ),
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final String entityName = getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getDomainModel()
				.getEntityDescriptor( entityName );
		entityDescriptor.applySqlSelections(
				getNavigablePath(),
				creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( getNavigablePath() ),
				creationState
		);

	}
}
