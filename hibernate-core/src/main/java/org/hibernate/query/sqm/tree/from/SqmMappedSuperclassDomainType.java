/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.query.sqm.SqmPathSource;

public interface SqmMappedSuperclassDomainType<T> extends MappedSuperclassDomainType<T>, SqmPathSource<T>, SqmDomainType<T> {
	@Override
	SqmMappedSuperclassDomainType<T> getSqmType();

	@Override
	default String getTypeName() {
		return SqmDomainType.super.getTypeName();
	}
}
