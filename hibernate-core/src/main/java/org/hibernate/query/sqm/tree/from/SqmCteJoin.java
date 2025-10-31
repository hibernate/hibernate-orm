/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCteJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.JoinType;

import java.util.Objects;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmCteJoin<T> extends AbstractSqmJoin<T, T> implements SqmSingularValuedJoin<T, T> {
	private final SqmCteStatement<T> cte;

	public SqmCteJoin(
			SqmCteStatement<T> cte,
			@Nullable String alias,
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
			@Nullable String alias,
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
		return (SqmRoot<?>) castNonNull( super.getLhs() );
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Override
	public @Nullable SqmFrom<?,T> getLhs() {
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
	public SqmCorrelatedCteJoin<T> createCorrelation() {
		return new SqmCorrelatedCteJoin<>( this );
	}

	@Override
	public @Nullable PersistentAttribute<? super T, ?> getAttribute() {
		return null;
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched) {
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
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& Objects.equals( cte.getCteTable().getCteName(), ((SqmCteJoin<?>) object).cte.getCteTable().getCteName() );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& Objects.equals( cte.getCteTable().getCteName(), ((SqmCteJoin<?>) object).cte.getCteTable().getCteName() );
	}
}
