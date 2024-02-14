/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.domain.AbstractSqmQualifiedJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin<T> extends AbstractSqmQualifiedJoin<T, T> implements JpaEntityJoin<T> {
	private final SqmRoot<?> sqmRoot;

	public SqmEntityJoin(
			EntityDomainType<T> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot<?> sqmRoot) {
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
			EntityDomainType<T> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot<?> sqmRoot) {
		super( navigablePath, joinedEntityDescriptor, sqmRoot, alias, joinType, sqmRoot.nodeBuilder() );
		this.sqmRoot = sqmRoot;
	}

	@Override
	public boolean isImplicitlySelectable() {
		return true;
	}

	@Override
	public SqmEntityJoin<T> copy(SqmCopyContext context) {
		final SqmEntityJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEntityJoin<T> path = context.registerCopy(
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

	public SqmRoot<?> getRoot() {
		return sqmRoot;
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}

	@Override
	public EntityDomainType<T> getModel() {
		return (EntityDomainType<T>) super.getModel();
	}

	@Override
	public SqmPath<?> getLhs() {
		// An entity-join has no LHS
		return null;
	}

	@Override
	public EntityDomainType<T> getReferencedPathSource() {
		return (EntityDomainType<T>) super.getReferencedPathSource();
	}

	public String getEntityName() {
		return getReferencedPathSource().getHibernateEntityName();
	}

	@Override
	public SqmEntityJoin<T> on(JpaExpression<Boolean> restriction) {
		return (SqmEntityJoin<T>) super.on( restriction );
	}

	@Override
	public SqmEntityJoin<T> on(Expression<Boolean> restriction) {
		return (SqmEntityJoin<T>) super.on( restriction );
	}

	@Override
	public SqmEntityJoin<T> on(JpaPredicate... restrictions) {
		return (SqmEntityJoin<T>) super.on( restrictions );
	}

	@Override
	public SqmEntityJoin<T> on(Predicate... restrictions) {
		return (SqmEntityJoin<T>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedEntityJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public <S extends T> SqmTreatedEntityJoin<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}
	@Override
	public <S extends T> SqmTreatedEntityJoin<T,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		final SqmTreatedEntityJoin<T,S> treat = findTreat( treatTarget, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedEntityJoin<>( this, treatTarget, null ) );
		}
		return treat;
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Entity join treats can not be aliased" );
	}

	@Override
	public SqmCorrelatedEntityJoin<T> createCorrelation() {
		return new SqmCorrelatedEntityJoin<>( this );
	}

	public SqmEntityJoin<T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmEntityJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				pathRegistry.findFromByPath( getRoot().getNavigablePath() )
		);
	}
}
