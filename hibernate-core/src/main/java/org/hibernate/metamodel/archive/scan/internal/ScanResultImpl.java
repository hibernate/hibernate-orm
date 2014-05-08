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
package org.hibernate.metamodel.archive.scan.internal;

import java.util.Set;

import org.hibernate.metamodel.archive.scan.spi.ClassDescriptor;
import org.hibernate.metamodel.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.metamodel.archive.scan.spi.PackageDescriptor;
import org.hibernate.metamodel.archive.scan.spi.ScanResult;

/**
* @author Steve Ebersole
*/
public class ScanResultImpl implements ScanResult {
	private final Set<PackageDescriptor> packageDescriptorSet;
	private final Set<ClassDescriptor> classDescriptorSet;
	private final Set<MappingFileDescriptor> mappingFileSet;

	public ScanResultImpl(
			Set<PackageDescriptor> packageDescriptorSet,
			Set<ClassDescriptor> classDescriptorSet,
			Set<MappingFileDescriptor> mappingFileSet) {
		this.packageDescriptorSet = packageDescriptorSet;
		this.classDescriptorSet = classDescriptorSet;
		this.mappingFileSet = mappingFileSet;
	}

	@Override
	public Set<PackageDescriptor> getLocatedPackages() {
		return packageDescriptorSet;
	}

	@Override
	public Set<ClassDescriptor> getLocatedClasses() {
		return classDescriptorSet;
	}

	@Override
	public Set<MappingFileDescriptor> getLocatedMappingFiles() {
		return mappingFileSet;
	}
}
