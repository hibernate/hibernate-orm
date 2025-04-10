/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.MapPersistentAttribute;

import java.util.Map;

public interface SqmMapPersistentAttribute<D, K, V>
		extends MapPersistentAttribute<D, K, V>, SqmPluralPersistentAttribute<D, Map<K, V>, V> {
}
