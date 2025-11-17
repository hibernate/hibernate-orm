/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

/**
 * Basic implementation of {@link TypeDefinitionRegistry}.
 *
 * @deprecated Internal code should use the internal implementation class
 * {@link org.hibernate.boot.internal.TypeDefinitionRegistryStandardImpl}.
 * This class will be removed.
 */
@Deprecated(since = "7.0", forRemoval = true)
public class TypeDefinitionRegistryStandardImpl
		extends org.hibernate.boot.internal.TypeDefinitionRegistryStandardImpl {

	public TypeDefinitionRegistryStandardImpl() {
		super();
	}

	public TypeDefinitionRegistryStandardImpl(TypeDefinitionRegistry parent) {
		super(parent);
	}

}
