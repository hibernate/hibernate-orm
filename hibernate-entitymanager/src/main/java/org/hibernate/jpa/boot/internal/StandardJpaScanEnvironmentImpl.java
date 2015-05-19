/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
				? Collections.<String>emptyList()
				: persistenceUnitDescriptor.getManagedClassNames();
		this.explicitlyListedMappingFiles = persistenceUnitDescriptor.getMappingFileNames() == null
				? Collections.<String>emptyList()
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
