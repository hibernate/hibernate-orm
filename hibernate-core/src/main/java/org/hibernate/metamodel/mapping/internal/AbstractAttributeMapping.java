/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

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
	@Nullable private final String name;
	private final int fetchableIndex;
	private final int stateArrayPosition;

	@Nullable private final ManagedMappingType declaringType;
	@Nullable private final AttributeMetadata attributeMetadata;
	@Nullable private final PropertyAccess propertyAccess;

	public AbstractAttributeMapping(
			@Nullable String name,
			int fetchableIndex,
			@Nullable ManagedMappingType declaringType,
			@Nullable AttributeMetadata attributeMetadata,
			int stateArrayPosition,
			@Nullable PropertyAccess propertyAccess) {
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

	@Nullable
	@Override
	public ManagedMappingType getDeclaringType() {
		return declaringType;
	}

	@Nullable
	@Override
	public String getAttributeName() {
		return name;
	}

	@Nullable
	@Override
	public AttributeMetadata getAttributeMetadata() {
		return attributeMetadata;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Nullable
	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return getMappedType();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return getMappedType().getMappedJavaType();
	}

	void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor){
	}
}
