/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.Serializable;

/**
 * A JavaBean bulk accessor, which provides methods capable of getting/setting multiple properties
 * of a JavaBean at once.
 *
 * IMPORTANT NOTE!!! Apparently the order of the methods here is important as I think BulkAccessorFactory
 * makes use of that information in terms of accessing the constructor.  Anyway, when I tried to re-arrange them
 * the BulkAccessor creation failed and tests started to fail.
 *
 * @author Muga Nishizawa
 * @author Shigeru Chiba
 */
public abstract class BulkAccessor implements Serializable {
	protected Class target;
	protected String[] getters, setters;
	protected Class[] types;

	/**
	 * Protected access constructor so the generated class has access to it.
	 */
	protected BulkAccessor() {
	}

	/**
	 * Obtains the values of properties of a given bean.
	 *
	 * @param bean   JavaBean.
	 * @param values the obtained values are stored in this array.
	 */
	public abstract void getPropertyValues(Object bean, Object[] values);

	/**
	 * Sets properties of a given bean to specified values.
	 *
	 * @param bean   JavaBean.
	 * @param values the values assinged to properties.
	 */
	public abstract void setPropertyValues(Object bean, Object[] values);

	/**
	 * Returns the values of properties of a given bean.
	 *
	 * @param bean JavaBean.
	 *
	 * @return The property values
	 */
	public Object[] getPropertyValues(Object bean) {
		final Object[] values = new Object[getters.length];
		getPropertyValues( bean, values );
		return values;
	}

	/**
	 * Returns the types of properties.
	 *
	 * @return The property types
	 */
	public Class[] getPropertyTypes() {
		return types.clone();
	}

	/**
	 * Returns the setter names of properties.
	 *
	 * @return The getter names
	 */
	public String[] getGetters() {
		return getters.clone();
	}

	/**
	 * Returns the getter names of the properties.
	 *
	 * @return The setter names
	 */
	public String[] getSetters() {
		return setters.clone();
	}

	/**
	 * Creates a new instance of <code>BulkAccessor</code>.
	 * The created instance provides methods for setting/getting
	 * specified properties at once.
	 *
	 * @param beanClass the class of the JavaBeans accessed
	 *                  through the created object.
	 * @param getters   the names of setter methods for specified properties.
	 * @param setters   the names of getter methods for specified properties.
	 * @param types     the types of specified properties.
	 *
	 * @return The created BulkAccessor
	 */
	public static BulkAccessor create(
			Class beanClass,
			String[] getters,
			String[] setters,
			Class[] types) {
		return new BulkAccessorFactory( beanClass, getters, setters, types ).create();
	}
}
