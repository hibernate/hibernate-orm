/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.criteria.PluralJoin;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * Base support for joins to plural attributes
 *
 * @param <L> The left-hand side of the join
 * @param <C> The collection type
 * @param <E> The collection's element type
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPluralJoin<L,C,E>
		extends AbstractSqmAttributeJoin<L,E>
		implements JpaJoin<L,E>, PluralJoin<L,C,E> {

	public AbstractSqmPluralJoin(
			SqmFrom<?, L> lhs,
			SqmPluralPersistentAttribute<? super L,C,E> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super(
				lhs,
				joinedNavigable.createNavigablePath( lhs, alias ),
				joinedNavigable,
				alias,
				joinType,
				fetched,
				nodeBuilder
		);
	}

	protected AbstractSqmPluralJoin(
			SqmFrom<?, L> lhs,
			NavigablePath navigablePath,
			PluralPersistentAttribute<L,C,E> joinedNavigable,
			@Nullable String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, (SqmPathSource<E>) joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SqmPluralPersistentAttribute<L, C, E> getModel() {
//		return (SqmPluralPersistentAttribute<L, C, E>) super.getNodeType();
		return (SqmPluralPersistentAttribute<L, C, E>) super.getModel();
	}
}
