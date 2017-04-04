/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for value types to hold collections and have cascades, etc.  The notion is that of composition.  JPA terms
 * this an embeddable.
 *
 * @author Steve Ebersole
 */
public interface CompositeType extends Type {
	/**
	 * Get the types of the component properties
	 *
	 * @return The component property types.
	 */
	Type[] getSubtypes();

	/**
	 * Get the names of the component properties
	 *
	 * @return The component property names
	 */
	String[] getPropertyNames();

	/**
	 * Retrieve the indicators regarding which component properties are nullable.
	 * <p/>
	 * An optional operation
	 *
	 * @return nullability of component properties
	 */
	boolean[] getPropertyNullability();

	/**
	 * Extract the values of the component properties from the given component instance
	 *
	 * @param component The component instance
	 * @param session The session from which the request originates
	 *
	 * @return The property values
	 *
	 * @throws HibernateException Indicates a problem access the property values.
	 */
	Object[] getPropertyValues(Object component, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Extract the values of the component properties from the given component instance without access to the
	 * session.
	 * <p/>
	 * An optional operation
	 *
	 * @param component The component instance
	 * @param entityMode The entity mode
	 *
	 * @return The property values
	 *
	 * @throws HibernateException Indicates a problem access the property values.
	 */
	Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException;

	/**
	 * Extract a particular component property value indicated by index.
	 *
	 * @param component The component instance
	 * @param index The index of the property whose value is to be extracted
	 * @param session The session from which the request originates.
	 *
	 * @return The extracted component property value
	 *
	 * @throws HibernateException Indicates a problem access the property value.
	 */
	Object getPropertyValue(Object component, int index, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Inject property values onto the given component instance
	 * <p/>
	 * An optional operation
	 *
	 * @param component The component instance
	 * @param values The values to inject
	 * @param entityMode The entity mode
	 *
	 * @throws HibernateException Indicates an issue performing the injection
	 */
	void setPropertyValues(Object component, Object[] values, EntityMode entityMode) throws HibernateException;

	/**
	 * Retrieve the cascade style of the indicated component property.
	 *
	 * @param index The property index,
	 *
	 * @return The cascade style.
	 */
	CascadeStyle getCascadeStyle(int index);

	/**
	 * Retrieve the fetch mode of the indicated component property.
	 *
	 * @param index The property index,
	 *
	 * @return The fetch mode
	 */
	FetchMode getFetchMode(int index);

	/**
	 * Is the given method a member of this component's class?
	 *
	 * @param method The method to check
	 *
	 * @return True if the method is a member; false otherwise.
	 */
	boolean isMethodOf(Method method);

	/**
	 * Is this component embedded?  "embedded" indicates that the component is "virtual", that its properties are
	 * "flattened" onto its owner
	 *
	 * @return True if this component is embedded; false otherwise.
	 */
	boolean isEmbedded();

	/**
	 * Convenience method to quickly check {@link #getPropertyNullability} for any non-nullable sub-properties.
	 *
	 * @return {@code true} if any of the properties are not-nullable as indicated by {@link #getPropertyNullability},
	 * {@code false} otherwise.
	 */
	boolean hasNotNullProperty();

	/**
	 * Convenience method for locating the property index for a given property name.
	 *
	 * @param propertyName The (sub-)property name to find.
	 *
	 * @return The (sub-)property index, relative to all the array-valued method returns defined on this contract.
	 */
	int getPropertyIndex(String propertyName);
}
