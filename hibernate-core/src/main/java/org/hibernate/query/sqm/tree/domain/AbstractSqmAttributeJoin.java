/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.JoinType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeJoin<L, R>
		extends AbstractSqmJoin<L, R>
		implements SqmAttributeJoin<L, R> {

	private final boolean implicitJoin;
	private boolean fetchJoin;

	protected AbstractSqmAttributeJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			SqmPathSource<R> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetchJoin,
			NodeBuilder nodeBuilder) {
		super(
				navigablePath,
				joinedNavigable,
				lhs,
				isImplicitAlias( alias ) ? null : alias,
				joinType,
				nodeBuilder
		);
		this.fetchJoin = fetchJoin;
		validateFetchAlias( alias, false, nodeBuilder );
		implicitJoin = isImplicitAlias( alias ); //TODO: add a parameter
	}

	@SuppressWarnings("StringEquality")
	private static boolean isImplicitAlias(@Nullable String alias) {
		return alias == SqmCreationHelper.IMPLICIT_ALIAS;
	}

	@Override
	public boolean isImplicitJoin() {
		return implicitJoin;
	}

	@Override
	public @NonNull SqmFrom<?, L> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Override
	public @NonNull JavaType<R> getNodeJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public boolean isFetched() {
		return fetchJoin;
	}

	@Override
	public SqmAttributeJoin<L,R> alias(String name) {
		validateFetchAlias( name, fetchJoin, nodeBuilder() );
		return (SqmAttributeJoin<L, R>) super.alias( name );
	}

	@Override
	public void clearFetched() {
		fetchJoin = false;
	}

	private static void validateFetchAlias(@Nullable String alias, boolean fetchJoin, NodeBuilder nodeBuilder) {
		if ( fetchJoin && alias != null && !alias.startsWith( "var_" )
				&& nodeBuilder.isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not permit specifying an alias for fetch joins."
			);
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedAttributeJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public @NonNull PersistentAttribute<? super L, ?> getAttribute() {
		//noinspection unchecked
		return (PersistentAttribute<? super L, ?>) getModel();
	}

	@Override
	public SqmAttributeJoin<L, R> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmAttributeJoin<L, R>) super.on( restriction );
	}

	@Override
	public SqmAttributeJoin<L, R> on(JpaPredicate @Nullable... restrictions) {
		return (SqmAttributeJoin<L, R>) super.on( restrictions );
	}

	@Override
	public @NonNull SqmFrom<?, L> getParent() {
		return getLhs();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L,R,S> treatAs(Class<S> treatJavaType);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(Class<S> treatJavaType, @Nullable String alias);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetched);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetched);

	// No need for equals/hashCode or isCompatible/cacheHashCode, because the base implementation using NavigablePath
	// is fine for the purpose of matching nodes "syntactically".

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
				&& object instanceof AbstractSqmAttributeJoin<?, ?> thatJoin
				&& fetchJoin == thatJoin.isFetched();
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& object instanceof AbstractSqmAttributeJoin<?, ?> thatJoin
			&& fetchJoin == thatJoin.isFetched();
	}
}
