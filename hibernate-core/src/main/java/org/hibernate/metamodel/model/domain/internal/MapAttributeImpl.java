/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
class MapAttributeImpl<X, K, V> extends AbstractPluralAttribute<X, Map<K, V>, V> implements MapPersistentAttribute<X, K, V> {
	private final SimpleDomainType<K> keyType;

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
	public SimpleDomainType<K> getKeyType() {
		return keyType;
	}

	@Override
	public SimpleDomainType<K> getKeyGraphType() {
		return getKeyType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception();
	}
}
