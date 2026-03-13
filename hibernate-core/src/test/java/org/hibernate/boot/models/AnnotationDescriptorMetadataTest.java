/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.hibernate.models.internal.AnnotationHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationTarget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AnnotationDescriptorMetadataTest {

	@Test
	public void testHibernateAnnotationsMetadata() {
		testAnnotationRegistry( "HibernateAnnotations", HibernateAnnotations::forEachAnnotation );
	}

	@Test
	public void testJpaAnnotationsMetadata() {
		testAnnotationRegistry( "JpaAnnotations", JpaAnnotations::forEachAnnotation );
	}

	@Test
	public void testXmlAnnotationsMetadata() {
		testAnnotationRegistry( "XmlAnnotations", XmlAnnotations::forEachAnnotation );
	}

	@Test
	public void testDialectOverrideAnnotationsMetadata() {
		testAnnotationRegistry( "DialectOverrideAnnotations", DialectOverrideAnnotations::forEachAnnotation );
	}

	private void testAnnotationRegistry(
			String registryName,
			Consumer<Consumer<AnnotationDescriptor<?>>> forEachAnnotation) {
		final AtomicInteger count = new AtomicInteger();
		final AtomicInteger failures = new AtomicInteger();
		final StringBuilder errorLog = new StringBuilder();

		forEachAnnotation.accept( descriptor -> {
			count.incrementAndGet();
			try {
				verifyDescriptor( descriptor );
			}
			catch (AssertionError e) {
				failures.incrementAndGet();
				errorLog.append( "\n  - " ).append( e.getMessage() );
			}
		} );

		if ( failures.get() > 0 ) {
			fail( String.format(
					"%s: %d of %d descriptors have incorrect metadata:%s",
					registryName,
					failures.get(),
					count.get(),
					errorLog
			) );
		}

		System.out.printf( "%s: All %d descriptors verified successfully%n", registryName, count.get() );
	}

	private void verifyDescriptor(AnnotationDescriptor<?> descriptor) {
		Class<? extends Annotation> annotationType = descriptor.getAnnotationType();
		String annotationName = annotationType.getName();

		EnumSet<AnnotationTarget.Kind> expectedTargets = AnnotationHelper.extractTargets(annotationType);
		EnumSet<AnnotationTarget.Kind> actualTargets = descriptor.getAllowableTargets();

		assertEquals(
				expectedTargets,
				actualTargets,
				String.format(
						"%s: Targets mismatch - expected %s but got %s",
						annotationName,
						formatTargets(expectedTargets),
						formatTargets(actualTargets)
				)
		);

		boolean expectedInherited = AnnotationHelper.isInherited(annotationType);
		boolean actualInherited = descriptor.isInherited();

		assertEquals(
				expectedInherited,
				actualInherited,
				String.format(
						"%s: Inherited mismatch - expected %s but got %s",
						annotationName,
						expectedInherited,
						actualInherited
				)
		);
	}

	private String formatTargets(EnumSet<AnnotationTarget.Kind> targets) {
		if (targets.size() == AnnotationTarget.Kind.values().length) {
			return "ALL";
		}
		return targets.toString();
	}
}
