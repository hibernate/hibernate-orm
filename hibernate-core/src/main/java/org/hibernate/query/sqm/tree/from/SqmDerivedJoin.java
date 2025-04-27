/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmDerivedJoin<T> extends AbstractSqmJoin<T, T> implements JpaDerivedJoin<T> {
	private final SqmSubQuery<T> subQuery;
	private final boolean lateral;

	public SqmDerivedJoin(
			SqmSubQuery<T> subQuery,
			String alias,
			SqmJoinType joinType,
			boolean lateral,
			SqmRoot<T> sqmRoot) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
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
		final SqmDerivedJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		final SqmDerivedJoin<T> path = context.registerCopy(
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

	public SqmRoot<?> getRoot() {
		return (SqmRoot<?>) super.getLhs();
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	public SqmSubQuery<T> getQueryPart() {
		return subQuery;
	}

	@Override
	public boolean isLateral() {
		return lateral;
	}

	@Override
	public SqmFrom<?,T> getLhs() {
		// A derived-join has no LHS
		return null;
	}

	@Override
	public SqmDerivedJoin<T> on(JpaExpression<Boolean> restriction) {
		return (SqmDerivedJoin<T>) super.on( restriction );
	}

	@Override
	public SqmDerivedJoin<T> on(Expression<Boolean> restriction) {
		return (SqmDerivedJoin<T>) super.on( restriction );
	}

	@Override
	public SqmDerivedJoin<T> on(JpaPredicate... restrictions) {
		return (SqmDerivedJoin<T>) super.on( restrictions );
	}

	@Override
	public SqmDerivedJoin<T> on(Predicate... restrictions) {
		return (SqmDerivedJoin<T>) super.on( restrictions );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedDerivedJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCorrelatedEntityJoin<T,T> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatTarget) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(Class<S> treatJavaType, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedJoin<T, T, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetched) {
		throw new UnsupportedOperationException( "Derived joins can not be treated" );
	}

	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		// none
		return null;
	}

	@Override
	public SqmFrom<?, T> getParent() {
		//noinspection unchecked
		return (SqmFrom<?, T>) getRoot();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmDerivedJoin<?> that
			&& super.equals( object )
			&& this.lateral == that.lateral
			&& Objects.equals( this.subQuery, that.subQuery );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), subQuery, lateral );
	}
}
