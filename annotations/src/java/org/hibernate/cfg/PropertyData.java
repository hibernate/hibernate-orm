package org.hibernate.cfg;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;

public interface PropertyData {

	/**
	 * @return default member access (whether field or property)
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	public String getDefaultAccess();

	/**
	 * @return property name
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	public String getPropertyName() throws MappingException;

	/**
	 * Returns the returned class itself or the element type if an array
	 */
	public XClass getClassOrElement() throws MappingException;

	/**
	 * Return the class itself
	 */
	public XClass getPropertyClass() throws MappingException;

	/**
	 * Returns the returned class name itself or the element type if an array
	 */
	public String getClassOrElementName() throws MappingException;

	/**
	 * Returns the returned class name itself
	 */
	public String getTypeName() throws MappingException;

	public XProperty getProperty();
}
