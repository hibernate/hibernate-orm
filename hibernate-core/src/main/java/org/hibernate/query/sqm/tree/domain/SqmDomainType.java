/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmBindableType;

@Incubating
public interface SqmDomainType<T>
		extends DomainType<T>, SqmBindableType<T> {

	@Override
	default String getTypeName() {
		return SqmBindableType.super.getTypeName();
	}

	default int getTupleLength() {
		return 1;
	}
}
