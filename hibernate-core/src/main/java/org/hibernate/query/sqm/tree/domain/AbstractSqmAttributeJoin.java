/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

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

import java.util.Objects;

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
			String alias,
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
		validateFetchAlias( alias );
		implicitJoin = isImplicitAlias( alias ); //TODO: add a parameter
	}

	@SuppressWarnings("StringEquality")
	private static boolean isImplicitAlias(String alias) {
		return alias == SqmCreationHelper.IMPLICIT_ALIAS;
	}

	@Override
	public boolean isImplicitJoin() {
		return implicitJoin;
	}

	@Override
	public SqmFrom<?, L> getLhs() {
		return super.getLhs();
	}

	@Override
	public JavaType<R> getNodeJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public boolean isFetched() {
		return fetchJoin;
	}

	@Override
	public SqmAttributeJoin<L,R> alias(String name) {
		validateFetchAlias( name );
		return (SqmAttributeJoin<L, R>) super.alias( name );
	}

	@Override
	public void clearFetched() {
		fetchJoin = false;
	}

	private void validateFetchAlias(String alias) {
//		if ( fetchJoin && alias != null && nodeBuilder().isJpaQueryComplianceEnabled() ) {
//			throw new IllegalStateException(
//					"The JPA specification does not permit specifying an alias for fetch joins."
//			);
//		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedAttributeJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public PersistentAttribute<? super L, ?> getAttribute() {
		//noinspection unchecked
		return (PersistentAttribute<? super L, ?>) getModel();
	}

	@Override
	public SqmAttributeJoin<L, R> on(JpaExpression<Boolean> restriction) {
		return (SqmAttributeJoin<L, R>) super.on( restriction );
	}

	@Override
	public SqmAttributeJoin<L, R> on(JpaPredicate... restrictions) {
		return (SqmAttributeJoin<L, R>) super.on( restrictions );
	}

	@Override
	public SqmFrom<?, L> getParent() {
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
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, String alias);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(Class<S> treatJavaType, String alias, boolean fetched);

	@Override
	public abstract <S extends R> SqmTreatedAttributeJoin<L, R, S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetched);

	@Override
	public boolean equals(Object object) {
		return object instanceof AbstractSqmAttributeJoin<?, ?> that
			&& super.equals( object )
			&& this.implicitJoin == that.implicitJoin
			&& this.fetchJoin == that.fetchJoin;
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), implicitJoin, fetchJoin );
	}
}
