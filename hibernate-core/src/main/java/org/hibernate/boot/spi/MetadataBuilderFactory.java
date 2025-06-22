/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.JavaServiceLoadable;

/**
 * An extension point for integrators that wish to hook into the process of how a {@link Metadata} is built. Intended as
 * a "discoverable service" (a {@link java.util.ServiceLoader}). There can be at most one implementation discovered that
 * returns a non-null {@link org.hibernate.boot.MetadataBuilder}.
 *
 * @author Gunnar Morling
 */
@JavaServiceLoadable
public interface MetadataBuilderFactory {

	/**
	 * Creates a {@link MetadataBuilderImplementor}.
	 *
	 * @param metadatasources The current metadata sources
	 * @param defaultBuilder The default builder, may be used as a delegate
	 * @return a new metadata builder
	 */
	MetadataBuilderImplementor getMetadataBuilder(MetadataSources metadatasources, MetadataBuilderImplementor defaultBuilder);
}
