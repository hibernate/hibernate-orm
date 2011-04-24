/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.jndi.spi;

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
