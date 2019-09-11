/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeMapping implements AttributeMapping {
	private final String name;

	private final MappingType type;
	private final ManagedMappingType declaringType;

	@SuppressWarnings("WeakerAccess")
	public AbstractAttributeMapping(String name, MappingType type, ManagedMappingType declaringType) {
		this.name = name;
		this.type = type;
		this.declaringType = declaringType;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return type;
	}

	@Override
	public ManagedMappingType getDeclaringType() {
		return declaringType;
	}
}
