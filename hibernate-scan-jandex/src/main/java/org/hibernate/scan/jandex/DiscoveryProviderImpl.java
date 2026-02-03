/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.spi.Discovery;
import org.hibernate.jpa.boot.discovery.spi.DiscoveryProvider;
import org.jboss.jandex.IndexView;

import java.util.Map;

/// Jandex-based implementation of ScannerProvider.
///
/// @author Steve Ebersole
public class DiscoveryProviderImpl implements DiscoveryProvider {

	@Override
	public Discovery builderScanner(
			ArchiveDescriptorFactory archiveDescriptorFactory,
			ClassLoaderService classLoaderService,
			Map properties) {
		return new DiscoveryImpl( archiveDescriptorFactory, classLoaderService, locateExistingJandexIndex( properties ) );
	}

	private IndexView locateExistingJandexIndex(Map properties) {
		return null;
	}
}
