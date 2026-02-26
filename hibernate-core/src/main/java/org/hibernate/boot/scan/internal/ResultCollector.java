/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.internal.util.StringHelper;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/// In-flight collection of [scan results][ScanningResult].
///
/// @author Steve Ebersole
public class ResultCollector {
	private final Set<String> discoveredPackages = new HashSet<>();
	private final Set<String> discoveredClasses = new HashSet<>();
	private final Set<URI> discoveredMappings = new HashSet<>();

	public void addPackage(String packageName) {
		discoveredPackages.add( packageName );
	}

	public void addClass(String className) {
		if ( className.endsWith( "package-info" ) ) {
			addPackage( StringHelper.qualifier( className ) );
		}
		else {
			discoveredClasses.add( className );
		}
	}

	public void addMapping(URI mappingUri) {
		discoveredMappings.add(mappingUri);
	}


	public ScanningResult toResult() {
		return new ScanningResultImpl(
				Collections.unmodifiableSet( discoveredPackages ),
				Collections.unmodifiableSet( discoveredClasses ),
				Collections.unmodifiableSet( discoveredMappings )
		);
	}
}
