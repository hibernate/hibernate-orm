/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.net.URL;

import org.hibernate.boot.ResourceLocator;

/**
 * Simple ResourceLocator impl using its own ClassLoader to locate the resource
 *
 * @author Steve Ebersole
 */
public class SimpleResourceLocator implements ResourceLocator {
	/**
	 * Singleton access
	 */
	public static final SimpleResourceLocator INSTANCE = new SimpleResourceLocator();

	@Override
	public URL locateResource(String resourceName) {
		return getClass().getClassLoader().getResource( resourceName );
	}
}
