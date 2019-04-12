/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractPluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class MapAttributeImpl<X, K, V>
		extends AbstractPluralPersistentAttribute<X, Map<K, V>, V>
		implements MapPersistentAttribute<X, K, V> {

	public MapAttributeImpl(
			PersistentCollectionDescriptor collectionDescriptor,
			Property bootProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		super( collectionDescriptor, bootProperty, propertyAccess, creationContext );
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.MAP;
	}

	@Override
	public Class<K> getKeyJavaType() {
		return getKeyType().getJavaType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SimpleTypeDescriptor<K> getKeyType() {
		return (SimpleTypeDescriptor<K>) getPersistentCollectionDescriptor().getKeyDomainTypeDescriptor();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<K, V> replaceElements(
			Map<K, V> originalValue,
			Map<K, V> targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		targetValue.clear();

		final CollectionIndex<K> indexDescriptor = getCollectionDescriptor().getIndexDescriptor();
		final CollectionElement<V> elementDescriptor = getCollectionDescriptor().getElementDescriptor();
		for ( Map.Entry<K, V> entry : originalValue.entrySet() ) {
			K key = indexDescriptor.replace( entry.getKey(), null, owner, copyCache, session );
			V value = elementDescriptor.replace( entry.getValue(), null, owner, copyCache, session );
			targetValue.put( key, value );
		}

		return targetValue;
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		//noinspection unchecked
		return new SqmMapJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}
}
