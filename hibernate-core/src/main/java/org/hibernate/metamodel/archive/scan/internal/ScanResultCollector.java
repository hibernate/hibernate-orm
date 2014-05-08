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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.metamodel.archive.scan.spi.ClassDescriptor;
import org.hibernate.metamodel.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.metamodel.archive.scan.spi.PackageDescriptor;
import org.hibernate.metamodel.archive.scan.spi.ScanEnvironment;
import org.hibernate.metamodel.archive.scan.spi.ScanOptions;
import org.hibernate.metamodel.archive.scan.spi.ScanParameters;
import org.hibernate.metamodel.archive.scan.spi.ScanResult;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;

import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ScanResultCollector {
	private static final Logger log = Logger.getLogger( ScanResultCollector.class );

	private final ScanEnvironment environment;
	private final ScanOptions options;

	private final ScanParameters scanParameters;

	private final Set<ClassDescriptor> discoveredClasses;
	private final Set<PackageDescriptor> discoveredPackages;
	private final Set<MappingFileDescriptor> discoveredMappingFiles;

	public ScanResultCollector(ScanEnvironment environment, ScanOptions options, ScanParameters parameters) {
		this.environment = environment;
		this.options = options;

		this.scanParameters = parameters;

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
		final ClassInfo classInfo = scanParameters.getJandexInitializer().handle( classDescriptor );

		// see if "discovery" of this entry is allowed

		if ( !isListedOrDetectable( classInfo.name().toString(), rootUrl ) ) {
			return;
		}

		if ( !containsClassAnnotationsOfInterest( classInfo ) ) {
			// not strictly needed, but helps cut down on the size of discoveredClasses
			return;
		}

		discoveredClasses.add( classDescriptor );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	protected boolean isListedOrDetectable(String name, boolean rootUrl) {
		// IMPL NOTE : protect the calls to getExplicitlyListedClassNames unless needed,
		// since it can take time with lots of listed classes.
		if ( rootUrl ) {
			// the entry comes from the root url, allow it if either:
			//		1) we are allowed to discover classes/packages in the root url
			//		2) the entry was explicitly listed
			return options.canDetectUnlistedClassesInRoot()
					|| environment.getExplicitlyListedClassNames().contains( name );
		}
		else {
			// the entry comes from a non-root url, allow it if either:
			//		1) we are allowed to discover classes/packages in non-root urls
			//		2) the entry was explicitly listed
			return options.canDetectUnlistedClassesInNonRoot()
					|| environment.getExplicitlyListedClassNames().contains( name );
		}
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean containsClassAnnotationsOfInterest(ClassInfo classInfo) {
		if ( classInfo.annotations() == null ) {
			return false;
		}

		return classInfo.annotations().containsKey( JPADotNames.ENTITY )
				|| classInfo.annotations().containsKey( JPADotNames.MAPPED_SUPERCLASS )
				|| classInfo.annotations().containsKey( JPADotNames.EMBEDDABLE )
				|| classInfo.annotations().containsKey( JPADotNames.CONVERTER );
	}

	public void handlePackage(PackageDescriptor packageDescriptor, boolean rootUrl) {
		final ClassInfo classInfo = scanParameters.getJandexInitializer().handle( packageDescriptor );

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
