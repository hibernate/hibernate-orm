/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedCteJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.JoinType;

import java.util.Objects;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;

/**
 * @author Christian Beikov
 */
@Incubating(since = "6.2")
public class SqmCteJoin<T> extends AbstractSqmJoin<T, T> implements SqmSingularValuedJoin<T, T> {
	private final SqmCteStatement<T> cte;

	public SqmCteJoin(
			@Nonnull SqmCteStatement<T> cte,
			@Nullable String alias,
			@Nonnull SqmJoinType joinType,
			@Nonnull SqmRoot<T> sqmRoot) {
		//noinspection unchecked
		this(
				buildRootNavigablePath( "<<cte>>", alias ),
				cte,
				(SqmPathSource<T>) cte.getCteTable().getTupleType(),
				alias,
				joinType,
				sqmRoot
		);
	}

	protected SqmCteJoin(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmCteStatement<T> cte,
			@Nonnull SqmPathSource<T> pathSource,
			@Nullable String alias,
			@Nonnull SqmJoinType joinType,
			@Nonnull SqmRoot<T> sqmRoot) {
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

	@Nonnull
	@Override
	public SqmCteJoin<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		final var path = context.registerCopy(
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

	@Nonnull
	public SqmRoot<?> getRoot() {
		return (SqmRoot<?>) castNonNull( super.getLhs() );
	}

	@Override
	@Nonnull
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Nonnull
	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Nullable
	@Override
	public SqmFrom<?,T> getLhs() {
		// A cte-join has no LHS
		return null;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedCteJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	@Nonnull
	public SqmCorrelatedCteJoin<T> createCorrelation() {
		return new SqmCorrelatedCteJoin<>( this );
	}

	@Nullable
	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		return null;
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	@Nonnull
	public SqmFrom<?, T> getParent() {
		return getCorrelationParent();
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& Objects.equals( cte.getCteTable().getCteName(),
				((SqmCteJoin<?>) object).cte.getCteTable().getCteName() );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& Objects.equals( cte.getCteTable().getCteName(),
				((SqmCteJoin<?>) object).cte.getCteTable().getCteName() );
	}
}
