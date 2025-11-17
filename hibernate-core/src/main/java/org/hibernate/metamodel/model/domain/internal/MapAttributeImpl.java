/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapPersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class MapAttributeImpl<X, K, V>
		extends AbstractPluralAttribute<X, Map<K, V>, V>
		implements SqmMapPersistentAttribute<X, K, V> {
	private final SqmPathSource<K> keyPathSource;

	public MapAttributeImpl(PluralAttributeBuilder<X, Map<K, V>, V, K> xceBuilder) {
		super( xceBuilder );
		keyPathSource = SqmMappingModelHelper.resolveSqmKeyPathSource(
				xceBuilder.getListIndexOrMapKeyType(),
				BindableType.PLURAL_ATTRIBUTE,
				xceBuilder.isGeneric()
		);
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.MAP;
	}

	@Override
	public Class<K> getKeyJavaType() {
		return keyPathSource.getBindableJavaType();
	}

	@Override
	public SqmPathSource<K> getKeyPathSource() {
		return keyPathSource;
	}

	@Override
	public SqmPathSource<K> getIndexPathSource() {
		return getKeyPathSource();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( name );
		if ( nature != null ) {
			switch ( nature ) {
				case INDEX:
					return keyPathSource;
				case ELEMENT:
					return getElementPathSource();
			}
		}
		return getElementPathSource().findSubPathSource( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return CollectionPart.Nature.INDEX.getName().equals( name )
				? keyPathSource
				: super.findSubPathSource( name, includeSubtypes );
	}

	@Override
	public SqmPathSource<?> getIntermediatePathSource(SqmPathSource<?> pathSource) {
		final String pathName = pathSource.getPathName();
		return pathName.equals( getElementPathSource().getPathName() )
			|| pathName.equals( keyPathSource.getPathName() ) ? null : getElementPathSource();
	}

	@Override
	public SimpleDomainType<K> getKeyType() {
		return (SimpleDomainType<K>) keyPathSource.getPathType();
	}

	@Override
	public SimpleDomainType<K> getKeyGraphType() {
		return getKeyType();
	}

	@Override
	public SqmAttributeJoin<X,V> createSqmJoin(
			SqmFrom<?,X> lhs, SqmJoinType joinType, String alias, boolean fetched, SqmCreationState creationState) {
		return new SqmMapJoin<>(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
