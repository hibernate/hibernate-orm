/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;


public interface SqmPluralPersistentAttribute<L, C, E>
		extends PluralPersistentAttribute<L, C, E>, SqmPathSource<E> {
	@Override
	SqmPathSource<E> getElementPathSource();
}
