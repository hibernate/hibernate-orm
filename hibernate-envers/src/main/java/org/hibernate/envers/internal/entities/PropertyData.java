/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.util.Objects;

import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.type.Type;

/**
 * The runtime representation of an audited property.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class PropertyData {
	private final String name;
	/**
	 * Name of the property in the bean.
	 */
	private final String beanName;
	private final String accessType;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean synthetic;
	private Type propertyType;
	private Class<?> virtualReturnClass;
	private PropertyAccessStrategy propertyAccessStrategy;

	/**
	 * Copies the given property data, except the name.
	 *
	 * @param newName New name.
	 * @param propertyData Property data to copy the rest of properties from.
	 */
	public PropertyData(String newName, PropertyData propertyData) {
		this.name = newName;
		this.beanName = propertyData.beanName;
		this.accessType = propertyData.accessType;

		this.usingModifiedFlag = propertyData.usingModifiedFlag;
		this.modifiedFlagName = propertyData.modifiedFlagName;
		this.synthetic = propertyData.synthetic;
		this.propertyType = propertyData.propertyType;
		this.virtualReturnClass = propertyData.virtualReturnClass;
	}

	/**
	 * @param name Name of the property.
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 */
	public PropertyData(String name, String beanName, String accessType) {
		this.name = name;
		this.beanName = beanName;
		this.accessType = accessType;
	}

	public PropertyData(String name, String beanName, String accessType, Type propertyType) {
		this( name, beanName, accessType );
		this.propertyType = propertyType;
	}

	/**
	 * @param name Name of the property.
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 * @param usingModifiedFlag Defines if field changes should be tracked
	 */
	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic) {
		this( name, beanName, accessType );
		this.usingModifiedFlag = usingModifiedFlag;
		this.modifiedFlagName = modifiedFlagName;
		this.synthetic = synthetic;
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			Type propertyType,
			PropertyAccessStrategy propertyAccessStrategy) {
		this( name, beanName, accessType, usingModifiedFlag, modifiedFlagName, synthetic, propertyType, null, propertyAccessStrategy );
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			Type propertyType,
			Class<?> virtualReturnClass,
			PropertyAccessStrategy propertyAccessStrategy) {
		this( name, beanName, accessType, usingModifiedFlag, modifiedFlagName, synthetic );
		this.propertyType = propertyType;
		this.virtualReturnClass = virtualReturnClass;
		this.propertyAccessStrategy = propertyAccessStrategy;
	}

	public String getName() {
		return name;
	}

	public String getBeanName() {
		return beanName;
	}

	public String getAccessType() {
		return accessType;
	}

	public boolean isUsingModifiedFlag() {
		return usingModifiedFlag;
	}

	public String getModifiedFlagPropertyName() {
		return modifiedFlagName;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	public Type getType() {
		return propertyType;
	}

	public Class<?> getVirtualReturnClass() {
		return virtualReturnClass;
	}

	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return propertyAccessStrategy;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final PropertyData that = (PropertyData) o;
		return usingModifiedFlag == that.usingModifiedFlag
				&& Objects.equals( accessType, that.accessType )
				&& Objects.equals( beanName, that.beanName )
				&& Objects.equals( name, that.name )
				&& Objects.equals( synthetic, that.synthetic );
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (beanName != null ? beanName.hashCode() : 0);
		result = 31 * result + (accessType != null ? accessType.hashCode() : 0);
		result = 31 * result + (usingModifiedFlag ? 1 : 0);
		result = 31 * result + (synthetic ? 1 : 0);
		return result;
	}

	public static PropertyData forProperty(String propertyName, Type propertyType) {
		return new PropertyData(
				propertyName,
				null,
				null,
				propertyType
		);
	}
}
