/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

@Incubating
public interface SqmManagedDomainType<J> extends ManagedDomainType<J>, SqmDomainType<J> {
	@Override
	String getTypeName();

	@Override
	@Nonnull
	default Class<J> getJavaType() {
		return ManagedDomainType.super.getJavaType();
	}

	@Override
	@Nullable SqmPersistentAttribute<? super J, ?> findAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPersistentAttribute<?, ?> findSubTypesAttribute(@Nonnull String name);

	@Override
	@Nullable SqmSingularPersistentAttribute<? super J, ?> findSingularAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPersistentAttribute<J, ?> findDeclaredAttribute(@Nonnull String name);

	@Override
	@Nullable SqmSingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPersistentAttribute<? super J, ?> findConcreteGenericAttribute(@Nonnull String name);

	@Override
	@Nullable SqmPersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(@Nonnull String name);
}
