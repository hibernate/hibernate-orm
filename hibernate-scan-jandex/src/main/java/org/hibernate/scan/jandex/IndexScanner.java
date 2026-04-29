/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.boot.scan.internal.ResultCollector;
import org.jboss.jandex.IndexView;

/// Scans the Jandex Index finding all "managed" classes.
/// As of JPA 4, this means classes with annotations which are defined with the
/// [@Discoverable][Discoverable] annotation.
///
/// @author Steve Ebersole
public class IndexScanner {
	/// Find the managed classes and add them to the result collector.
	///
	/// @param jandexIndex The Jandex index to scan.
	/// @param resultCollector The collector of results.
	public static void scanForClasses(IndexView jandexIndex, ResultCollector resultCollector) {
		// Find all uses of the `@Discoverable` annotation.  This will be a list of annotations
		// we actually want to scan for.
		var discoverableUses = jandexIndex.getAnnotations( Discoverable.class );

		// `discoverableUses` are the annotations, annotated with `@Discoverable`, for which we want to scan
		discoverableUses.forEach( discoverableUse -> {
			var discoveredAnnotationType = discoverableUse.target().asClass();
			var discoveredAnnotationUses = jandexIndex.getAnnotations( discoveredAnnotationType.name() );
			discoveredAnnotationUses.forEach( discoveredAnnotationUse -> {
				resultCollector.addClass( discoveredAnnotationUse.target().asClass().name().toString() );
			} );
		} );
	}
}
