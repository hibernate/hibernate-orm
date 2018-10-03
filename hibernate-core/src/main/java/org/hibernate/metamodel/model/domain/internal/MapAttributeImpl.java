/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.MapAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;

/**
 * @author Steve Ebersole
 */
class MapAttributeImpl<X, K, V> extends AbstractPluralAttribute<X, Map<K, V>, V>
		implements MapAttributeImplementor<X, K, V> {
	private final SimpleTypeImplementor<K> keyType;

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
	public SimpleTypeImplementor<K> getKeyType() {
		return keyType;
	}

	@Override
	public SimpleTypeImplementor<K> getKeyGraphType() {
		return getKeyType();
	}
}
