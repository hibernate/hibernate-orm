/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

public class PropertyPreloadedData implements PropertyData {
	private final AccessType defaultAccess;

	private final String propertyName;

	private final TypeDetails returnedClass;

	public PropertyPreloadedData(AccessType defaultAccess, String propertyName, TypeDetails returnedClass) {
		this.defaultAccess = defaultAccess;
		this.propertyName = propertyName;
		this.returnedClass = returnedClass;
	}

	PropertyPreloadedData() {
		this( null, null, null );
	}

	@Override
	public AccessType getDefaultAccess() throws MappingException {
		return defaultAccess;
	}

	@Override
	public String getPropertyName() throws MappingException {
		return propertyName;
	}

	@Override
	public TypeDetails getClassOrElementType() throws MappingException {
		return getPropertyType();
	}

	@Override
	public ClassDetails getClassOrPluralElement() throws MappingException {
		return getPropertyType().determineRawClass();
	}

	@Override
	public TypeDetails getPropertyType() throws MappingException {
		return returnedClass;
	}

	@Override
	public String getClassOrElementName() throws MappingException {
		return getTypeName();
	}

	@Override
	public String getTypeName() throws MappingException {
		return returnedClass == null ? null : returnedClass.getName();
	}

	@Override
	public MemberDetails getAttributeMember() {
		return null; //instead of UnsupportedOperationException
	}

	@Override
	public ClassDetails getDeclaringClass() {
		//Preloaded properties are artificial wrapper for collection element accesses
		//and idClass creation, ignore.
		return null;
	}
}
