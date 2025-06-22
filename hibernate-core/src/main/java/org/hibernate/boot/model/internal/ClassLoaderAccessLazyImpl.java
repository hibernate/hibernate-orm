/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

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
