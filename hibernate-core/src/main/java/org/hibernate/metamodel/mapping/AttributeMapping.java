/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Describes an attribute at the mapping model level.
 *
 * @author Steve Ebersole
 */
public interface AttributeMapping extends ModelPart, ValueMapping {
	String getAttributeName();

	@Override
	default String getPartName() {
		return getAttributeName();
	}

	AttributeMetadataAccess getAttributeMetadataAccess();

	ManagedMappingType getDeclaringType();

	PropertyAccess getPropertyAccess();

	@Override
	default EntityMappingType findContainingEntityMapping() {
		return getDeclaringType().findContainingEntityMapping();
	}
}
