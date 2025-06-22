/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;

/**
 * Implementation of ScanEnvironment leveraging a JPA deployment descriptor.
 *
 * @author Steve Ebersole
 */
public class StandardJpaScanEnvironmentImpl implements ScanEnvironment {
	private final PersistenceUnitDescriptor persistenceUnitDescriptor;

	private final List<String> explicitlyListedClassNames;
	private final List<String> explicitlyListedMappingFiles;


	public StandardJpaScanEnvironmentImpl(PersistenceUnitDescriptor persistenceUnitDescriptor) {
		this.persistenceUnitDescriptor = persistenceUnitDescriptor;

		this.explicitlyListedClassNames = persistenceUnitDescriptor.getManagedClassNames() == null
				? Collections.emptyList()
				: persistenceUnitDescriptor.getManagedClassNames();
		this.explicitlyListedMappingFiles = persistenceUnitDescriptor.getMappingFileNames() == null
				? Collections.emptyList()
				: persistenceUnitDescriptor.getMappingFileNames();
	}

	@Override
	public URL getRootUrl() {
		return persistenceUnitDescriptor.getPersistenceUnitRootUrl();
	}

	@Override
	public List<URL> getNonRootUrls() {
		return persistenceUnitDescriptor.getJarFileUrls();
	}

	@Override
	public List<String> getExplicitlyListedClassNames() {
		return explicitlyListedClassNames;
	}

	@Override
	public List<String> getExplicitlyListedMappingFiles() {
		return explicitlyListedMappingFiles;
	}
}
