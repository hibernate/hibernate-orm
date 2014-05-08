/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi;

import java.net.URL;

/**
 * During the process of building this metamodel, accessing the ClassLoader
 * is very discouraged.  However, sometimes it is needed.  This contract helps
 * mitigate accessing the ClassLoader in these cases.
 *
 * @author Steve Ebersole
 */
public interface ClassLoaderAccess {
	/**
	 * Obtain a Class reference by name
	 *
	 * @param name The name of the Class to get a reference to.
	 *
	 * @return The Class.
	 */
	public <T> Class<T> classForName(String name);

	public URL locateResource(String resourceName);
}
