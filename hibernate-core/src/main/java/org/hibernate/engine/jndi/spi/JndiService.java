/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jndi.spi;

import javax.naming.event.NamespaceChangeListener;

import org.hibernate.service.Service;

/**
 * Service providing simplified access to {@literal JNDI} related features needed by Hibernate.
 *
 * @author Steve Ebersole
 */
public interface JndiService extends Service {
	/**
	 * Locate an object in {@literal JNDI} by name
	 *
	 * @param jndiName The {@literal JNDI} name of the object to locate
	 *
	 * @return The object found (may be null).
	 */
	public Object locate(String jndiName);

	/**
	 * Binds a value into {@literal JNDI} by name.
	 *
	 * @param jndiName The name under which to bind the object
	 * @param value The value to bind
	 */
	public void bind(String jndiName, Object value);

	/**
	 * Unbind a value from {@literal JNDI} by name.
	 *
	 * @param jndiName The name under which the object is bound
	 */
	public void unbind(String jndiName);

	/**
	 * Adds the specified listener to the given {@literal JNDI} namespace.
	 *
	 * @param jndiName The {@literal JNDI} namespace
	 * @param listener The listener
	 */
	public void addListener(String jndiName, NamespaceChangeListener listener);
}
