/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.query.sqm.SqmPathSource;

@Incubating
public interface SqmMappedSuperclassDomainType<T>
		extends MappedSuperclassDomainType<T>, SqmPathSource<T>, SqmManagedDomainType<T> {
	@Override
	@Nullable SqmDomainType<T> getSqmType();

	@Override
	String getTypeName();
}
