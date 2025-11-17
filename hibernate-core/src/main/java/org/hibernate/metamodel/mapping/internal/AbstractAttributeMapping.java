/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeMapping implements AttributeMapping {
	private final String name;
	private final int fetchableIndex;
	private final int stateArrayPosition;

	private final ManagedMappingType declaringType;
	private final AttributeMetadata attributeMetadata;
	private final PropertyAccess propertyAccess;

	public AbstractAttributeMapping(
			String name,
			int fetchableIndex,
			ManagedMappingType declaringType,
			AttributeMetadata attributeMetadata,
			int stateArrayPosition,
			PropertyAccess propertyAccess) {
		this.name = name;
		this.fetchableIndex = fetchableIndex;
		this.declaringType = declaringType;
		this.attributeMetadata = attributeMetadata;
		this.stateArrayPosition = stateArrayPosition;
		this.propertyAccess = propertyAccess;
	}

	/**
	 * For Hibernate Reactive
	 */
	protected AbstractAttributeMapping(AbstractAttributeMapping original) {
		this(
				original.name,
				original.fetchableIndex,
				original.declaringType,
				original.attributeMetadata,
				original.stateArrayPosition,
				original.propertyAccess
		);
	}

	@Override
	public ManagedMappingType getDeclaringType() {
		return declaringType;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public AttributeMetadata getAttributeMetadata() {
		return attributeMetadata;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Override
	public MappingType getPartMappingType() {
		return getMappedType();
	}

	@Override
	public JavaType<?> getJavaType() {
		return getMappedType().getMappedJavaType();
	}

	void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor){
	}
}
