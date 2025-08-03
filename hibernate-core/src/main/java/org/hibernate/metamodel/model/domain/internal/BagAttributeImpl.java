/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmBagPersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class BagAttributeImpl<X, E>
		extends AbstractPluralAttribute<X, Collection<E>, E>
		implements SqmBagPersistentAttribute<X, E> {

	public BagAttributeImpl(PluralAttributeBuilder<X, Collection<E>, E, ?> xceBuilder) {
		super( xceBuilder );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.COLLECTION;
	}

	@Override
	public SqmAttributeJoin<X,E> createSqmJoin(
			SqmFrom<?,X> lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return new SqmBagJoin<>(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
