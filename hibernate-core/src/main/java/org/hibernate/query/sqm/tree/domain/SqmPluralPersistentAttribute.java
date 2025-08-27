/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;


@Incubating
public interface SqmPluralPersistentAttribute<D, C, E>
		extends PluralPersistentAttribute<D, C, E>, SqmPersistentAttribute<D,C>, SqmJoinable<D,E>, SqmPathSource<E> {
	@Override
	SqmPathSource<E> getElementPathSource();
}
