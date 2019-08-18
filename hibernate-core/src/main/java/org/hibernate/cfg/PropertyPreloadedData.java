/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

public class PropertyPreloadedData implements PropertyData {
	private final AccessType defaultAccess;

	private final String propertyName;

	private final XClass returnedClass;

	public PropertyPreloadedData(AccessType defaultAccess, String propertyName, XClass returnedClass) {
		this.defaultAccess = defaultAccess;
		this.propertyName = propertyName;
		this.returnedClass = returnedClass;
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
	public XClass getClassOrElement() throws MappingException {
		return getPropertyClass();
	}

	@Override
	public XClass getPropertyClass() throws MappingException {
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
	public XProperty getProperty() {
		return null; //instead of UnsupportedOperationException
	}

	@Override
	public XClass getDeclaringClass() {
		//Preloaded properties are artificial wrapper for colleciton element accesses
		//and idClass creation, ignore.
		return null;
	}
}
