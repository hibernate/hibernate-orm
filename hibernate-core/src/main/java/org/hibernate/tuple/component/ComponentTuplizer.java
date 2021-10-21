/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.component;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.tuple.Tuplizer;

/**
 * Defines further responsibilities regarding tuplization based on
 * a mapped components.
 * </p>
 * ComponentTuplizer implementations should have the following constructor signature:
 *      (org.hibernate.mapping.Component)
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface ComponentTuplizer extends Tuplizer, Serializable {

	/**
	 * Is the given method available via the managed component as a property getter?
	 *
	 * @param method The method which to check against the managed component.
	 * @return True if the managed component is available from the managed component; else false.
	 */
	public boolean isMethodOf(Method method);

	/**
	 * Generate a new, empty entity.
	 *
	 * @return The new, empty entity instance.
	 */
	public Object instantiate();

	/**
	 * Extract the current values contained on the given entity.
	 *
	 * @param entity The entity from which to extract values.
	 * @return The current property values.
	 */
	public Object[] getPropertyValues(Object entity);

	/**
	 * Inject the given values into the given entity.
	 *
	 * @param entity The entity.
	 * @param values The values to be injected.
	 */
	public void setPropertyValues(Object entity, Object[] values);
}
