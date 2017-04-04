/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.net.URL;

/**
 * A ClassLoaderAccess implementation based on delegation
 *
 * @author Steve Ebersole
 */
public abstract class ClassLoaderAccessDelegateImpl implements ClassLoaderAccess {
	protected abstract ClassLoaderAccess getDelegate();

	@Override
	public <T> Class<T> classForName(String name) {
		return getDelegate().classForName( name );
	}

	@Override
	public URL locateResource(String resourceName) {
		return getDelegate().locateResource( resourceName );
	}
}
