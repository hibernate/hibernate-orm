/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
class MapAttributeImpl<X, K, V> extends AbstractPluralAttribute<X, Map<K, V>, V> implements MapPersistentAttribute<X, K, V> {
	private final SqmPathSource<K> keyPathSource;

	MapAttributeImpl(PluralAttributeBuilder<X, Map<K, V>, V, K> xceBuilder, MetadataContext metadataContext) {
		super( xceBuilder, metadataContext );

		this.keyPathSource = DomainModelHelper.resolveSqmPathSource(
				getName(),
				xceBuilder.getListIndexOrMapKeyType(),
				BindableType.PLURAL_ATTRIBUTE
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
	public SqmPathSource getKeyPathSource() {
		return keyPathSource;
	}

	@Override
	public SqmPathSource getIndexPathSource() {
		return getKeyPathSource();
	}

	@Override
	public SimpleDomainType<K> getKeyType() {
		return (SimpleDomainType<K>) keyPathSource.getSqmPathType();
	}

	@Override
	public SimpleDomainType getKeyGraphType() {
		return getKeyType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs, SqmJoinType joinType, String alias, boolean fetched, SqmCreationState creationState) {
		return new SqmMapJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
