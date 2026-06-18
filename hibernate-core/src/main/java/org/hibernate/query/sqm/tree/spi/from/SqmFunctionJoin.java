/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.spi.expression.SqmSetReturningFunction;
import org.hibernate.spi.NavigablePath;

import java.util.List;



/**
 * @author Christian Beikov
 */
@Incubating
public class SqmFunctionJoin<E> extends AbstractSqmJoin<Object, E> implements JpaFunctionJoin<E> {
	private final SqmSetReturningFunction<E> function;
	private final boolean lateral;

	public SqmFunctionJoin(
			SqmSetReturningFunction<E> function,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean lateral,
			SqmFrom<?, Object> sqmFrom) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				function,
				lateral,
				function.getType(),
				alias,
				validateJoinType( joinType, lateral ),
				sqmFrom
		);
	}

	public SqmFunctionJoin(
			NavigablePath navigablePath,
			SqmSetReturningFunction<E> function,
			boolean lateral,
			SqmPathSource<E> pathSource,
			@Nullable String alias,
			SqmJoinType joinType,
			SqmFrom<?, Object> sqmFrom) {
		super(
				navigablePath,
				pathSource,
				sqmFrom,
				alias,
				joinType,
				sqmFrom.nodeBuilder()
		);
		this.function = function;
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
	public SqmFunctionJoin<E> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmFunctionJoin<>(
						getNavigablePath(),
						function,
						lateral,
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						getParent().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSetReturningFunction<E> getFunction() {
		return function;
	}

	@Override
	public SqmPath<Long> index() {
		//noinspection unchecked
		final SqmPathSource<Long> indexPathSource = (SqmPathSource<Long>) function.getType().getSubPathSource( CollectionPart.Nature.INDEX.getName() );
		return resolvePath( indexPathSource.getPathName(), indexPathSource );
	}

	@Override
	public boolean isLateral() {
		return lateral;
	}

	@Nullable
	@Override
	public SqmFrom<?, Object> getLhs() {
		// A derived-join has no LHS
		return null;
	}

	@Override
	@Nonnull
	public SqmFunctionJoin<E> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmFunctionJoin<E>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmFunctionJoin<E> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmFunctionJoin<E>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmFunctionJoin<E> on(@Nullable JpaPredicate... restrictions) {
		return (SqmFunctionJoin<E>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmFunctionJoin<E> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmFunctionJoin<E>) super.on( restrictions );
	}

	@Override
	@Nonnull
	public SqmFunctionJoin<E> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return (SqmFunctionJoin<E>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedFunctionJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	@Nonnull
	public SqmCorrelation<Object, E> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(@Nonnull Class<S> treatTarget) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(
			@Nonnull EntityDomainType<S> treatTarget,
			@Nullable String alias,
			boolean fetched) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Nullable
	@Override
	public PersistentAttribute<? super Object, ?> getAttribute() {
		// none
		return null;
	}

	@Override
	@Nonnull
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& object instanceof SqmFunctionJoin<?> that
			&& lateral == that.isLateral()
			&& function.equals( that.function );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& object instanceof SqmFunctionJoin<?> that
			&& lateral == that.isLateral()
			&& function.isCompatible( that.function );
	}
}
