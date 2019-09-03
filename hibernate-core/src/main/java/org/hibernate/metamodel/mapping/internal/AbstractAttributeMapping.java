/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.MappingType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeMapping implements AttributeMapping {
	private final String name;

	private final MappingType type;

	public AbstractAttributeMapping(String name, MappingType type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return type;
	}
}
