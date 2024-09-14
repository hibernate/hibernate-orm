/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collection;

import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class BagAttributeImpl<X, E>
		extends AbstractPluralAttribute<X, Collection<E>, E>
		implements BagPersistentAttribute<X, E> {

	public BagAttributeImpl(PluralAttributeBuilder<X, Collection<E>, E, ?> xceBuilder, MetadataContext metadataContext) {
		super( xceBuilder, metadataContext );
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
