/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

import java.util.Set;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanResult;


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
