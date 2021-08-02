/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Provides access to details about an attribute specific to a particular
 * entity in the hierarchy.  Accounts for attribute/association overrides, etc
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface AttributeMetadataAccess {
	/**
	 * Resolve the details about the attribute
	 */
	AttributeMetadata resolveAttributeMetadata(EntityMappingType entityMappingType);
}
