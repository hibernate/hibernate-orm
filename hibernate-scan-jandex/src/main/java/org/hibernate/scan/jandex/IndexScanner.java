package org.hibernate.scan.jandex;

import java.util.HashSet;
import org.jboss.jandex.AnnotationTarget;

import jakarta.persistence.spi.Discoverable;
import org.hibernate.boot.scan.internal.ResultCollector;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/// Scans the Jandex Index finding all "managed" classes and modules.
/// As of JPA 4, this means classes with annotations which are defined with the
/// [@Discoverable][Discoverable] annotation.
///
/// @author Steve Ebersole
public class IndexScanner {
	private static final DotName JAKARTA_DATA_REPOSITORY = DotName.createSimple( "jakarta.data.repository.Repository" );

	/// Classify discoverable program elements from the candidate index only.
	/// Lookup metadata may include annotation definitions outside the scan boundaries.
	public static void scanForResources(IndexView candidates, IndexView lookup, ResultCollector collector) {
		final var discoverable = new HashSet<DotName>();
		lookup.getAnnotations( Discoverable.class ).forEach( use -> {
			if ( use.target().kind() == AnnotationTarget.Kind.CLASS ) {
				discoverable.add( use.target().asClass().name() );
			}
		} );
		discoverable.add( JAKARTA_DATA_REPOSITORY );
		for ( var annotationName : discoverable ) {
			for ( var use : candidates.getAnnotations( annotationName ) ) {
				if ( use.target().kind() == AnnotationTarget.Kind.CLASS ) {
					final var type = use.target().asClass();
					if ( type.isModule() ) {
						collector.addModule( type.module().name().toString() );
					}
					else {
						collector.addClass( type.name().toString() );
					}
				}
			}
		}
		// Module descriptors have separate Jandex storage, including when several
		// archives each contain a classfile named module-info.
		for ( var module : candidates.getKnownModules() ) {
			// we only collect modules here which have annotations marked as "discoverable"
			if ( module.annotations()
					.stream()
					.anyMatch( annotation -> discoverable.contains( annotation.name() ) ) ) {
				collector.addModule( module.name().toString() );
			}
		}
	}
}
