/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetPersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SetAttributeImpl<X, E>
		extends AbstractPluralAttribute<X, Set<E>, E>
		implements SqmSetPersistentAttribute<X, E> {

	public SetAttributeImpl(PluralAttributeBuilder<X, Set<E>, E, ?> xceBuilder) {
		super( xceBuilder );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.SET;
	}

	@Override
	public SqmAttributeJoin<X, E> createSqmJoin(
			SqmFrom<?,X> lhs, SqmJoinType joinType, @Nullable String alias, boolean fetched, SqmCreationState creationState) {
		return new SqmSetJoin<>(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
