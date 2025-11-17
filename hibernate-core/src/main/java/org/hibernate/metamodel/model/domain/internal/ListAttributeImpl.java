/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmListPersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class ListAttributeImpl<X, E>
		extends AbstractPluralAttribute<X, List<E>, E>
		implements SqmListPersistentAttribute<X, E> {
	private final SqmPathSource<Integer> indexPathSource;

	public ListAttributeImpl(PluralAttributeBuilder<X, List<E>, E, ?> builder) {
		super( builder );

		//noinspection unchecked
		this.indexPathSource = (SqmPathSource<Integer>) SqmMappingModelHelper.resolveSqmKeyPathSource(
				builder.getListIndexOrMapKeyType(),
				BindableType.PLURAL_ATTRIBUTE,
				false
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
	public SqmPathSource<?> findSubPathSource(String name) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( name );
		if ( nature != null ) {
			switch ( nature ) {
				case INDEX:
					return indexPathSource;
				case ELEMENT:
					return getElementPathSource();
			}
		}
		return getElementPathSource().findSubPathSource( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return CollectionPart.Nature.INDEX.getName().equals( name )
				? indexPathSource
				: super.findSubPathSource( name, includeSubtypes );
	}

	@Override
	public SqmPathSource<?> getIntermediatePathSource(SqmPathSource<?> pathSource) {
		final String pathName = pathSource.getPathName();
		return pathName.equals( getElementPathSource().getPathName() )
			|| pathName.equals( indexPathSource.getPathName() ) ? null : getElementPathSource();
	}

	@Override
	public SqmAttributeJoin<X,E> createSqmJoin(
			SqmFrom<?,X> lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return new SqmListJoin<>(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
