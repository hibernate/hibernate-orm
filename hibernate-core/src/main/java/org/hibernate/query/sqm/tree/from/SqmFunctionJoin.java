/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmFunctionJoin<E> extends AbstractSqmJoin<Object, E> implements JpaFunctionJoin<E> {
	private final SqmSetReturningFunction<E> function;
	private final boolean lateral;

	public SqmFunctionJoin(
			SqmSetReturningFunction<E> function,
			String alias,
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
			String alias,
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
		final SqmFunctionJoin<E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFunctionJoin<E> path = context.registerCopy(
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

	@Override
	public SqmFrom<?, Object> getLhs() {
		// A derived-join has no LHS
		return null;
	}

	@Override
	public SqmFunctionJoin<E> on(JpaExpression<Boolean> restriction) {
		return (SqmFunctionJoin<E>) super.on( restriction );
	}

	@Override
	public SqmFunctionJoin<E> on(Expression<Boolean> restriction) {
		return (SqmFunctionJoin<E>) super.on( restriction );
	}

	@Override
	public SqmFunctionJoin<E> on(JpaPredicate... restrictions) {
		return (SqmFunctionJoin<E>) super.on( restrictions );
	}

	@Override
	public SqmFunctionJoin<E> on(Predicate... restrictions) {
		return (SqmFunctionJoin<E>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedFunctionJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCorrelation<Object, E> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(Class<S> treatTarget) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(Class<S> treatJavaType, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public <S extends E> SqmTreatedJoin<Object, E, S> treatAs(
			EntityDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		throw new UnsupportedOperationException( "Function joins can not be treated" );
	}

	@Override
	public PersistentAttribute<? super Object, ?> getAttribute() {
		// none
		return null;
	}

	@Override
	public @NonNull SqmFrom<?, Object> getParent() {
		return castNonNull( super.getParent() );
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmFunctionJoin<?> that
			&& super.equals( object )
			&& this.lateral == that.lateral
			&& Objects.equals( this.function, that.function );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), function, lateral );
	}
}
