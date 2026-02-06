/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.spi.Discovery;
import org.hibernate.jpa.boot.discovery.spi.DiscoveryProvider;

import java.util.Map;

/// Standard implementation of [DiscoveryProvider].
///
/// @author Steve Ebersole
public class StandardDiscoveryProvider implements DiscoveryProvider {
	@Override
	public Discovery builderScanner(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			ClassLoaderService classLoaderService,
			Map properties) {
		return new StandardDiscovery( archiveDescriptorFactory, classLoaderService );
	}
}
