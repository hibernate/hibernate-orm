/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.reflection;

import java.net.URL;

import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
 * A ClassLoaderAccess implementation based on lazy access to {@link MetadataBuildingOptions}
 *
 * @author Steve Ebersole
 */
public class ClassLoaderAccessLazyImpl implements ClassLoaderAccess {
	private final MetadataBuildingOptions metadataBuildingOptions;

	public ClassLoaderAccessLazyImpl(MetadataBuildingOptions metadataBuildingOptions) {
		this.metadataBuildingOptions = metadataBuildingOptions;
	}

	@Override
	public <T> Class<T> classForName(String name) {
		return null;
	}

	@Override
	public URL locateResource(String resourceName) {
		return null;
	}
}
