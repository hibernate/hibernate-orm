/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

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
	SqmPersistentAttribute<? super J, ?> findAttribute(String name);

	@Override @Deprecated(since = "7.0", forRemoval = true)
	default SqmPersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name) {
		return findAttribute( name );
	}

	@Override
	SqmPersistentAttribute<?, ?> findSubTypesAttribute(String name);

	@Override
	SqmSingularPersistentAttribute<? super J, ?> findSingularAttribute(String name);

	@Override
	SqmPluralPersistentAttribute<? super J, ?, ?> findPluralAttribute(String name);

	@Override
	SqmPersistentAttribute<J, ?> findDeclaredAttribute(String name);

	@Override
	SqmSingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name);

	@Override
	SqmPluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name);

	@Override
	SqmPersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name);

	@Override
	SqmPersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name);
}
