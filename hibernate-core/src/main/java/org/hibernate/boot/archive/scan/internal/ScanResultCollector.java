/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ScanResultCollector {
	private static final Logger log = Logger.getLogger( ScanResultCollector.class );

	private final ScanEnvironment environment;
	private final ScanOptions options;

	private final Set<ClassDescriptor> discoveredClasses;
	private final Set<PackageDescriptor> discoveredPackages;
	private final Set<MappingFileDescriptor> discoveredMappingFiles;

	public ScanResultCollector(ScanEnvironment environment, ScanOptions options, ScanParameters parameters) {
		this.environment = environment;
		this.options = options;

		if ( environment.getExplicitlyListedClassNames() == null ) {
			throw new IllegalArgumentException( "ScanEnvironment#getExplicitlyListedClassNames should not return null" );
		}

		if ( environment.getExplicitlyListedMappingFiles() == null ) {
			throw new IllegalArgumentException( "ScanEnvironment#getExplicitlyListedMappingFiles should not return null" );
		}

		this.discoveredPackages = new HashSet<PackageDescriptor>();
		this.discoveredClasses = new HashSet<ClassDescriptor>();
		this.discoveredMappingFiles = new HashSet<MappingFileDescriptor>();
	}

	public void handleClass(ClassDescriptor classDescriptor, boolean rootUrl) {
		if ( !isListedOrDetectable( classDescriptor.getName(), rootUrl ) ) {
			return;
		}

		discoveredClasses.add( classDescriptor );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	protected boolean isListedOrDetectable(String name, boolean rootUrl) {
		// IMPL NOTE : protect the calls to getExplicitlyListedClassNames unless needed,
		// since it can take time with lots of listed classes.
		if ( rootUrl ) {
			// The entry comes from the root url.  Allow it if either:
			//		1) we are allowed to discover classes/packages in the root url
			//		2) the entry was explicitly listed
			return options.canDetectUnlistedClassesInRoot()
					|| environment.getExplicitlyListedClassNames().contains( name );
		}
		else {
			// The entry comes from a non-root url.  Allow it if either:
			//		1) we are allowed to discover classes/packages in non-root urls
			//		2) the entry was explicitly listed
			return options.canDetectUnlistedClassesInNonRoot()
					|| environment.getExplicitlyListedClassNames().contains( name );
		}
	}

	public void handlePackage(PackageDescriptor packageDescriptor, boolean rootUrl) {
		if ( !isListedOrDetectable( packageDescriptor.getName(), rootUrl ) ) {
			// not strictly needed, but helps cut down on the size of discoveredPackages
			return;
		}

		discoveredPackages.add( packageDescriptor );
	}

	public void handleMappingFile(MappingFileDescriptor mappingFileDescriptor, boolean rootUrl) {
		if ( acceptAsMappingFile( mappingFileDescriptor, rootUrl ) ) {
			discoveredMappingFiles.add( mappingFileDescriptor );
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean acceptAsMappingFile(MappingFileDescriptor mappingFileDescriptor, boolean rootUrl) {
		if ( mappingFileDescriptor.getName().endsWith( "hbm.xml" ) ) {
			return options.canDetectHibernateMappingFiles();
		}

		if ( mappingFileDescriptor.getName().endsWith( "META-INF/orm.xml" ) ) {
			if ( environment.getExplicitlyListedMappingFiles().contains( "META-INF/orm.xml" ) ) {
				// if the user explicitly listed META-INF/orm.xml, only except the root one
				//
				// not sure why exactly, but this is what the old code does
				return rootUrl;
			}
			return true;
		}

		return environment.getExplicitlyListedMappingFiles().contains( mappingFileDescriptor.getName() );
	}

	public ScanResult toScanResult() {
		return new ScanResultImpl(
				Collections.unmodifiableSet( discoveredPackages ),
				Collections.unmodifiableSet( discoveredClasses ),
				Collections.unmodifiableSet( discoveredMappingFiles )
		);
	}
}
