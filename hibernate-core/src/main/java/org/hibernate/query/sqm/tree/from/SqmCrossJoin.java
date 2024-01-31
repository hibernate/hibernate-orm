/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedCrossJoin;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCrossJoin<T> extends AbstractSqmFrom<T, T> implements JpaCrossJoin<T>, SqmJoin<T, T> {
	private final SqmRoot<?> sqmRoot;

	public SqmCrossJoin(
			EntityDomainType<T> joinedEntityDescriptor,
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
			EntityDomainType<T> joinedEntityDescriptor,
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
	}

	@Override
	public boolean isImplicitlySelectable() {
		return true;
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
	public SqmPath<?> getLhs() {
		// a cross-join has no LHS
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
	public SqmJoinType getSqmJoinType() {
		return SqmJoinType.CROSS;
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

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public <S extends T> SqmTreatedCrossJoin<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedCrossJoin<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		final SqmTreatedCrossJoin<T, S> treat = findTreat( treatTarget, null );
		if ( treat == null ) {
			return addTreat( new SqmTreatedCrossJoin<>( this, treatTarget, null ) );
		}
		return treat;
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Cross join treats can not be aliased" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Cross join treats can not be aliased" );
	}

	public SqmCrossJoin<T> makeCopy(SqmCreationProcessingState creationProcessingState) {
		final SqmPathRegistry pathRegistry = creationProcessingState.getPathRegistry();
		return new SqmCrossJoin<>(
				getReferencedPathSource(),
				getExplicitAlias(),
				pathRegistry.findFromByPath( getRoot().getNavigablePath() )
		);
	}
}
