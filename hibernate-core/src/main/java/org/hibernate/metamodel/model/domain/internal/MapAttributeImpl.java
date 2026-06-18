/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapPersistentAttribute;
import org.hibernate.query.sqm.tree.spi.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;

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
	@Nonnull
	public CollectionType getCollectionType() {
		return CollectionType.MAP;
	}

	@Override
	@Nonnull
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
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
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
	public @Nullable SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
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
	@Nonnull
	public SimpleDomainType<K> getKeyType() {
		return (SimpleDomainType<K>) keyPathSource.getPathType();
	}

	@Override
	@Nonnull
	public SimpleDomainType<K> getKeyGraphType() {
		return getKeyType();
	}

	@Override
	public SqmAttributeJoin<X,V> createSqmJoin(
			SqmFrom<?,X> lhs, SqmJoinType joinType, @Nullable String alias, boolean fetched, SqmCreationState creationState) {
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
