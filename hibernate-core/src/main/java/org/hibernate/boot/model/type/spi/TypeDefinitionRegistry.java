/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import org.hibernate.boot.model.TypeDefinition;

/**
 * Defines a registry for TypeDefinitions based on registration keys.
 *
 * @author Chris Cranford
 */
public interface TypeDefinitionRegistry {
	enum DuplicationStrategy {
		KEEP,
		OVERWRITE,
		DISALLOW
	}

	TypeDefinition resolve(String typeName);

	TypeDefinitionRegistry register(TypeDefinition typeDefinition);
	TypeDefinitionRegistry register(TypeDefinition typeDefinition, DuplicationStrategy duplicationStrategy);
}
