/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
class ListAttributeImpl<X, E> extends AbstractPluralAttribute<X, List<E>, E> implements ListPersistentAttribute<X, E> {
	private final SqmPathSource<Integer> indexPathSource;

	ListAttributeImpl(PluralAttributeBuilder<X, List<E>, E, ?> builder) {
		super( builder );

		//noinspection unchecked
		this.indexPathSource = (SqmPathSource) DomainModelHelper.resolveSqmPathSource(
				getName(),
				builder.getListIndexOrMapKeyType(),
				BindableType.PLURAL_ATTRIBUTE
		);
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.LIST;
	}

	@Override
	public SqmPathSource<Integer> getIndexPathSource() {
		return indexPathSource;
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		//noinspection unchecked
		return new SqmListJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
