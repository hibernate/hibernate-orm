/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

@Incubating
public interface SqmManagedDomainType<J> extends ManagedDomainType<J>, SqmDomainType<J> {
	@Override
	String getTypeName();

	@Override
	default Class<J> getJavaType() {
		return ManagedDomainType.super.getJavaType();
	}

	@Override
	@Nullable SqmPersistentAttribute<? super J, ?> findAttribute(String name);

	@Override @Deprecated(since = "7.0", forRemoval = true)
	default @Nullable SqmPersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name) {
		return findAttribute( name );
	}

	@Override
	@Nullable SqmPersistentAttribute<?, ?> findSubTypesAttribute(String name);

	@Override
	@Nullable SqmSingularPersistentAttribute<? super J, ?> findSingularAttribute(String name);

	@Override
	@Nullable SqmPluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(String name);

	@Override
	@Nullable SqmPersistentAttribute<J, ?> findDeclaredAttribute(String name);

	@Override
	@Nullable SqmSingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name);

	@Override
	@Nullable SqmPluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name);

	@Override
	@Nullable SqmPersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name);

	@Override
	@Nullable SqmPersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name);
}
