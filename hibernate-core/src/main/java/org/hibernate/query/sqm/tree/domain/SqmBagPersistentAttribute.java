/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.BagPersistentAttribute;

import java.util.Collection;

public interface SqmBagPersistentAttribute<D, E>
		extends BagPersistentAttribute<D, E>, SqmPluralPersistentAttribute<D, Collection<E>, E> {
}
