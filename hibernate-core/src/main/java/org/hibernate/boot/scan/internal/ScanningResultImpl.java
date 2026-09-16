/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import java.util.LinkedHashSet;

import org.hibernate.boot.scan.spi.ScanningResult;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/// Standard implementation of [ScanningResult].
///
/// @author Steve Ebersole
public record ScanningResultImpl(
		Set<String> discoveredModules,
		Set<String> discoveredPackages,
		Set<String> discoveredClasses,
		Set<URI> mappingFiles) implements ScanningResult {

	public ScanningResultImpl {
		discoveredModules = Collections.unmodifiableSet( new LinkedHashSet<>( discoveredModules ) );
		discoveredPackages = Collections.unmodifiableSet( new LinkedHashSet<>( discoveredPackages ) );
		discoveredClasses = Collections.unmodifiableSet( new LinkedHashSet<>( discoveredClasses ) );
		mappingFiles = Collections.unmodifiableSet( new LinkedHashSet<>( mappingFiles ) );
	}

	public ScanningResultImpl() {
		this( Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet() );
	}


}
