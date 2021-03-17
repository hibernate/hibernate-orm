/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;

/**
 * @author Steve Ebersole
 */
class MapAttributeImpl<X, K, V> extends AbstractPluralAttribute<X, Map<K, V>, V>
		implements MapPersistentAttribute<X, K, V> {
	private final SimpleTypeDescriptor<K> keyType;

	MapAttributeImpl(PluralAttributeBuilder<X, Map<K, V>, V, K> xceBuilder) {
		super( xceBuilder );
		this.keyType = xceBuilder.getKeyType();
	}

	@Override
	public CollectionType getCollectionType() {
		return CollectionType.MAP;
	}

	@Override
	public Class<K> getKeyJavaType() {
		return keyType.getJavaType();
	}

	@Override
	public SimpleTypeDescriptor<K> getKeyType() {
		return keyType;
	}

	@Override
	public SimpleTypeDescriptor<K> getKeyGraphType() {
		return getKeyType();
	}
}
