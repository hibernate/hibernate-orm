/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.SetPersistentAttribute;

import java.util.Set;

public interface SqmSetPersistentAttribute<D, E>
		extends SetPersistentAttribute<D, E>, SqmPluralPersistentAttribute<D, Set<E>, E> {
}
