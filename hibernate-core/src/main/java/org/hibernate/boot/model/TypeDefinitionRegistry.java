/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import java.util.Map;

import org.hibernate.type.descriptor.java.BasicJavaType;

/**
 * @author Chris Cranford
 * @author Steve Ebersole
 */
public interface TypeDefinitionRegistry {

	enum DuplicationStrategy {
		KEEP,
		OVERWRITE,
		DISALLOW
	}

	TypeDefinition resolve(String typeName);
	TypeDefinition resolveAutoApplied(BasicJavaType<?> jtd);

	TypeDefinitionRegistry register(TypeDefinition typeDefinition);
	TypeDefinitionRegistry register(TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy);

	Map<String, TypeDefinition> copyRegistrationMap();
}
