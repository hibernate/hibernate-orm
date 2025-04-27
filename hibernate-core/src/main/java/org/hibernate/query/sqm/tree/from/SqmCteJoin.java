/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.JoinType;

import java.util.Objects;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmCteJoin<T> extends AbstractSqmJoin<T, T> {
	private final SqmCteStatement<T> cte;

	public SqmCteJoin(
			SqmCteStatement<T> cte,
			String alias,
			SqmJoinType joinType,
			SqmRoot<T> sqmRoot) {
		//noinspection unchecked
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<cte>>", alias ),
				cte,
				(SqmPathSource<T>) cte.getCteTable().getTupleType(),
				alias,
				joinType,
				sqmRoot
		);
	}

	protected SqmCteJoin(
			NavigablePath navigablePath,
			SqmCteStatement<T> cte,
			SqmPathSource<T> pathSource,
			String alias,
			SqmJoinType joinType,
			SqmRoot<T> sqmRoot) {
		super(
				navigablePath,
				pathSource,
				sqmRoot,
				alias,
				joinType,
				sqmRoot.nodeBuilder()
		);
		this.cte = cte;
	}

	@Override
	public boolean isImplicitlySelectable() {
		return false;
	}

	@Override
	public SqmCteJoin<T> copy(SqmCopyContext context) {
		final SqmCteJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		final SqmCteJoin<T> path = context.registerCopy(
				this,
				new SqmCteJoin<>(
						getNavigablePath(),
						cte.copy( context ),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						(SqmRoot<T>) findRoot().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmRoot<?> getRoot() {
		return (SqmRoot<?>) super.getLhs();
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Override
	public SqmFrom<?,T> getLhs() {
		// A cte-join has no LHS
		return null;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedCteJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCorrelatedEntityJoin<T,T> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		return null;
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public SqmFrom<?, T> getParent() {
		return getCorrelationParent();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmCteJoin<?> that
			&& super.equals( object )
			&& Objects.equals( this.cte.getName(), that.cte.getName() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), cte.getName() );
	}
}
