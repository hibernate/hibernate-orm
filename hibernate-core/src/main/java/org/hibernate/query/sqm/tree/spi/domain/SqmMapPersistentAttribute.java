package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.metamodel.model.domain.MapPersistentAttribute;

import java.util.Map;

public interface SqmMapPersistentAttribute<D, K, V>
		extends MapPersistentAttribute<D, K, V>, SqmPluralPersistentAttribute<D, Map<K, V>, V> {
}
