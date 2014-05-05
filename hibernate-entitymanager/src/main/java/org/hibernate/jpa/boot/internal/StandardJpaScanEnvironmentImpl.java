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
package org.hibernate.jpa.boot.internal;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;

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
