/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.List;

import static java.lang.String.join;

/// Failure-location diagnostic attached as a suppressed exception by the
/// cascade-plan walker.
///
/// The original failure remains the thrown exception so its concrete type,
/// message, cause, and structured entity information are preserved. This
/// diagnostic is stackless because the original failure already identifies the
/// execution stack; its purpose is only to add the compiled cascade location.
///
/// @author Steve Ebersole
final class CascadePathDiagnostic extends RuntimeException {
	private final String rootEntityName;
	private final List<String> path;

	CascadePathDiagnostic(String rootEntityName, List<String> path) {
		super( message( rootEntityName, path ), null, false, false );
		this.rootEntityName = rootEntityName;
		this.path = List.copyOf( path );
	}

	String rootEntityName() {
		return rootEntityName;
	}

	List<String> path() {
		return path;
	}

	private static String message(String rootEntityName, List<String> path) {
		return "Cascade processing failed at '" + rootEntityName
				+ ( path.isEmpty() ? "" : "." + join( ".", path ) ) + "'";
	}
}
