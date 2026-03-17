/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.scan.jandex;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import org.hibernate.boot.scan.internal.ResultCollector;
import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 */
public class IndexScanner {
	public static void scanForClasses(IndexView jandexIndex, ResultCollector resultCollector) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// the legacy behavior - look for specific annotations
		// todo (jpa4) : After https://github.com/jakartaee/persistence/pull/940 is available, rework this to use `@Discoverable`.

//		var discoverableUses = jandexIndex.getAnnotations( Discoverable.class );
//		discoverableUses.forEach( discoverableUse -> {
//			var discoveredAnnotationType = discoverableUse.target().asClass();
//			var discoveredAnnotationUses = jandexIndexToUse.getAnnotations( discoveredAnnotationType.name() );
//			discoveredAnnotationUses.forEach( discoveredAnnotationUse -> {
//				classNameConsumer.accept(  discoveredAnnotationUse.target().asClass().name().toString() );
//			} );
//		} );

		jandexIndex.getAnnotations( Entity.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndex.getAnnotations( MappedSuperclass.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );
		jandexIndex.getAnnotations( Embeddable.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndex.getAnnotations( Converter.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndex.getAnnotations( NamedQuery.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndex.getAnnotations( NamedQueries.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndex.getAnnotations( org.hibernate.annotations.NamedQuery.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );

		jandexIndex.getAnnotations( org.hibernate.annotations.NamedQueries.class ).forEach( annotationUsage -> {
			resultCollector.addClass( annotationUsage.target().asClass().name().toString() );
		} );
	}
}
