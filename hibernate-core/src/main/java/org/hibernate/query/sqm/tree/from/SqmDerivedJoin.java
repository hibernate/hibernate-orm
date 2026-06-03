/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedDerivedJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularValuedJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.spi.NavigablePath;

import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;


/**
 * @author Christian Beikov
 */
@Incubating
public class SqmDerivedJoin<T> extends AbstractSqmJoin<T, T> implements JpaDerivedJoin<T>, SqmSingularValuedJoin<T, T> {
	private final SqmSubQuery<T> subQuery;
	private final boolean lateral;

	public SqmDerivedJoin(
			SqmSubQuery<T> subQuery,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean lateral,
			SqmRoot<T> sqmRoot) {
		this(
				buildRootNavigablePath( "<<derived>>", alias ),
				subQuery,
				lateral,
				new AnonymousTupleType<>( subQuery ),
				alias,
				validateJoinType( joinType, lateral ),
				sqmRoot
		);
	}

	protected SqmDerivedJoin(
			NavigablePath navigablePath,
			SqmSubQuery<T> subQuery,
			boolean lateral,
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
		this.subQuery = subQuery;
		this.lateral = lateral;
	}

	private static SqmJoinType validateJoinType(SqmJoinType joinType, boolean lateral) {
		if ( lateral ) {
			switch ( joinType ) {
				case LEFT:
				case INNER:
					break;
				default:
					throw new IllegalArgumentException( "Lateral joins can only be left or inner. Illegal join type: " + joinType );
			}
		}
		return joinType;
	}

	@Override
	public boolean isImplicitlySelectable() {
		return false;
	}

	@Override
	public SqmDerivedJoin<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		final var path = context.registerCopy(
				this,
				new SqmDerivedJoin<>(
						getNavigablePath(),
						subQuery,
						lateral,
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
	@Override
	public SqmSubQuery<T> getQueryPart() {
		return subQuery;
	}

	@Override
	public boolean isLateral() {
		return lateral;
	}

	@Nullable
	@Override
	public SqmFrom<?,T> getLhs() {
		// A derived-join has no LHS
		return null;
	}

	@Override
	@Nonnull
	public SqmDerivedJoin<T> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmDerivedJoin<T>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmDerivedJoin<T> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmDerivedJoin<T>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmDerivedJoin<T> on(@Nullable JpaPredicate... restrictions) {
		return (SqmDerivedJoin<T>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmDerivedJoin<T> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmDerivedJoin<T>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmDerivedJoin<T> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return (SqmDerivedJoin<T>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedDerivedJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	@Nonnull
	public SqmCorrelatedDerivedJoin<T> createCorrelation() {
		return new SqmCorrelatedDerivedJoin<>( this );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull Class<S> treatTarget) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Nullable
	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		// none
		return null;
	}

	@Override
	@Nonnull
	public SqmFrom<?, T> getParent() {
		//noinspection unchecked
		return (SqmFrom<?, T>) getRoot();
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& object instanceof SqmDerivedJoin<?> that
			&& lateral == that.isLateral()
			&& subQuery.equals( that.subQuery );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& object instanceof SqmDerivedJoin<?> that
			&& lateral == that.isLateral()
			&& subQuery.isCompatible( that.subQuery );
	}
}
