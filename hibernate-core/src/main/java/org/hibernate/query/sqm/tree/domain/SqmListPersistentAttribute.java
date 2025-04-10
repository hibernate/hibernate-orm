/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.ListPersistentAttribute;

import java.util.List;

public interface SqmListPersistentAttribute<D, E>
		extends ListPersistentAttribute<D, E>, SqmPluralPersistentAttribute<D, List<E>, E> {
}
