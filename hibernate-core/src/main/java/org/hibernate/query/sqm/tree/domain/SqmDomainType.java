/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.SqmBindable;

@Incubating
public interface SqmDomainType<T>
		extends DomainType<T>, SqmBindable<T>, BindableType<T> {
	@Override
	SqmDomainType<T> getSqmType();

	@Override
	default String getTypeName() {
		return SqmBindable.super.getTypeName();
	}

	default int getTupleLength() {
		return 1;
	}

	@Override
	Class<T> getJavaType();
}
