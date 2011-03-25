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

import org.hibernate.service.Service;

/**
 * Service providing simplified access to JNDI related features needed by Hibernate.
 *
 * @author Steve Ebersole
 */
public interface JndiService extends Service {
	/**
	 * Locate an object in JNDI by name
	 *
	 * @param jndiName The JNDI name of the object to locate
	 *
	 * @return The object found (may be null).
	 */
	public Object locate(String jndiName);

	/**
	 * Binds a value into JNDI by name.
	 *
	 * @param jndiName The name under whcih to bind the object
	 * @param value The value to bind
	 */
	public void bind(String jndiName, Object value);
}
