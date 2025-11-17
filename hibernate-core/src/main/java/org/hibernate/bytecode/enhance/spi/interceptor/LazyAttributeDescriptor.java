/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.mapping.Property;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Descriptor for an attribute which is enabled for bytecode lazy fetching
 *
 * @author Steve Ebersole
 */
public class LazyAttributeDescriptor {
	public static LazyAttributeDescriptor from(
			Property property,
			int attributeIndex,
			int lazyIndex) {
		String fetchGroupName = property.getLazyGroup();
		if ( fetchGroupName == null ) {
			fetchGroupName = property.getType() instanceof CollectionType
					? property.getName()
					: "DEFAULT";
		}

		return new LazyAttributeDescriptor(
				attributeIndex,
				lazyIndex,
				property.getName(),
				property.getType(),
				fetchGroupName
		);
	}

	private final int attributeIndex;
	private final int lazyIndex;
	private final String name;
	private final Type type;
	private final String fetchGroupName;

	private LazyAttributeDescriptor(
			int attributeIndex,
			int lazyIndex,
			String name,
			Type type,
			String fetchGroupName) {
		assert attributeIndex >= lazyIndex;
		this.attributeIndex = attributeIndex;
		this.lazyIndex  = lazyIndex;
		this.name = name;
		this.type = type;
		this.fetchGroupName = fetchGroupName;
	}

	/**
	 * Access to the index of the attribute in terms of its position in the entity persister
	 *
	 * @return The persister attribute index
	 */
	public int getAttributeIndex() {
		return attributeIndex;
	}

	/**
	 * Access to the index of the attribute in terms of its position within the lazy attributes of the persister
	 *
	 * @return The persister lazy attribute index
	 */
	public int getLazyIndex() {
		return lazyIndex;
	}

	/**
	 * Access to the name of the attribute
	 *
	 * @return The attribute name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Access to the attribute's type
	 *
	 * @return The attribute type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Access to the name of the fetch group to which the attribute belongs
	 *
	 * @return The name of the fetch group the attribute belongs to
	 */
	public String getFetchGroupName() {
		return fetchGroupName;
	}
}
