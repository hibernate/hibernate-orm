/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.util.Objects;

import org.hibernate.envers.ModificationStore;
import org.hibernate.type.Type;

/**
 * Holds information on a property that is audited.
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
	private final ModificationStore store;
	private boolean usingModifiedFlag;
	private String modifiedFlagName;
	// Synthetic properties are ones which are not part of the actual java model.
	// They're properties used for bookkeeping by Hibernate
	private boolean synthetic;
	private Type propertyType;
	private Class<?> virtualReturnClass;

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
		this.store = propertyData.store;

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
	 * @param store How this property should be stored.
	 */
	public PropertyData(String name, String beanName, String accessType, ModificationStore store) {
		this.name = name;
		this.beanName = beanName;
		this.accessType = accessType;
		this.store = store;
	}

	private PropertyData(String name, String beanName, String accessType, ModificationStore store, Type propertyType) {
		this( name, beanName, accessType, store );
		this.propertyType = propertyType;
	}

	/**
	 * @param name Name of the property.
	 * @param beanName Name of the property in the bean.
	 * @param accessType Accessor type for this property.
	 * @param store How this property should be stored.
	 * @param usingModifiedFlag Defines if field changes should be tracked
	 */
	public PropertyData(
			String name,
			String beanName,
			String accessType,
			ModificationStore store,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic) {
		this( name, beanName, accessType, store );
		this.usingModifiedFlag = usingModifiedFlag;
		this.modifiedFlagName = modifiedFlagName;
		this.synthetic = synthetic;
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			ModificationStore store,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			Type propertyType) {
		this( name, beanName, accessType, store, usingModifiedFlag, modifiedFlagName, synthetic, propertyType, null );
	}

	public PropertyData(
			String name,
			String beanName,
			String accessType,
			ModificationStore store,
			boolean usingModifiedFlag,
			String modifiedFlagName,
			boolean synthetic,
			Type propertyType,
			Class<?> virtualReturnClass) {
		this( name, beanName, accessType, store, usingModifiedFlag, modifiedFlagName, synthetic );
		this.propertyType = propertyType;
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

	 /**
	 * @deprecated since 5.2, to be removed in 6.0 with no replacement.
	 */
	@Deprecated
	public ModificationStore getStore() {
		return store;
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
				&& store == that.store
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
		result = 31 * result + (store != null ? store.hashCode() : 0);
		result = 31 * result + (usingModifiedFlag ? 1 : 0);
		result = 31 * result + (synthetic ? 1 : 0);
		return result;
	}

	public static PropertyData forProperty(String propertyName, Type propertyType) {
		return new PropertyData(
				propertyName,
				null,
				null,
				null,
				propertyType
		);
	}
}
