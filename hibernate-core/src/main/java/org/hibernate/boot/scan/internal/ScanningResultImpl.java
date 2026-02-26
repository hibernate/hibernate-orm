/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.internal;

import org.hibernate.boot.scan.spi.ScanningResult;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/// Standard implementation of [ScanningResult].
///
/// @author Steve Ebersole
public record ScanningResultImpl(
		Set<String> discoveredPackages,
		Set<String> discoveredClasses,
		Set<URI> mappingFiles) implements ScanningResult {

	public ScanningResultImpl() {
		this( Collections.emptySet(), Collections.emptySet(), Collections.emptySet() );
	}


}
