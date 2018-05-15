/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.util.Objects;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Holds information on a property that is audited.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyData {
	private final String name;
	/**
	 * Name of the property in the bean.
	 */
	private final String beanName;
	private final String accessType;
	private final boolean usingModifiedFlag;
	private final String modifiedFlagName;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean synthetic;
	private Class<?> virtualReturnClass;
	private JavaTypeDescriptor javaTypeDescriptor;

	/**
	 * Copies the given property data, except the name.
	 *
	 * @param newName New name.
	 * @param propertyData Property data to copy the rest of properties from.
	 */
	public PropertyData(String newName, PropertyData propertyData) {
		this ( newName, propertyData.beanName, propertyData.accessType, propertyData.getJavaTypeDescriptor() );
	}

	/**
	 * @param name Name of the property
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 */
	public PropertyData(String name, String beanName, String accessType) {
		this( name, beanName, accessType, null );
	}

	/**
	 * @param name Name of the property.
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 * @param javaTypeDescriptor The java type descriptor.
	 */
	public PropertyData(String name, String beanName, String accessType, JavaTypeDescriptor javaTypeDescriptor) {
		this( name, beanName, accessType, false, null, false, javaTypeDescriptor, null );
	}

	/**
	 * @param name Name of the property.
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 * @param usingModifiedFlag Defines if field changes should be tracked
	 * @param synthetic Is the property a synthetic property
	 */
	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic) {
		this( name, beanName, accessType, usingModifiedFlag, modifiedFlagName, synthetic, null, null );
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			JavaTypeDescriptor javaTypeDescriptor) {
		this( name, beanName, accessType, usingModifiedFlag, modifiedFlagName, synthetic, javaTypeDescriptor, null );
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			JavaTypeDescriptor javaTypeDescriptor,
			Class<?> virtualReturnClass) {
		this.name = name;
		this.beanName = beanName;
		this.accessType = accessType;
		this.usingModifiedFlag = usingModifiedFlag;
		this.modifiedFlagName = modifiedFlagName;
		this.synthetic = synthetic;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.virtualReturnClass = virtualReturnClass;
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

	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public Class<?> getVirtualReturnClass() {
		return virtualReturnClass;
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
		result = 31 * result + ( beanName != null ? beanName.hashCode() : 0 );
		result = 31 * result + ( accessType != null ? accessType.hashCode() : 0 );
		result = 31 * result + ( usingModifiedFlag ? 1 : 0 );
		result = 31 * result + ( synthetic ? 1 : 0 );
		return result;
	}

	public static PropertyData forProperty(String propertyName, JavaTypeDescriptor javaTypeDescriptor) {
		return new PropertyData(
				propertyName,
				null,
				null,
				javaTypeDescriptor
		);
	}
}
