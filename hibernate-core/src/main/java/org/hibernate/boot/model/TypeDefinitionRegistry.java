/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
