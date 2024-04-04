/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.SourceModelBuildingContext;

public class AnnotationUsageHelper {

	public static <A extends Annotation> void applyAttributeIfSpecified(
			String attributeName,
			Object value,
			MutableAnnotationUsage<A> annotationUsage) {
		XmlAnnotationHelper.applyOptionalAttribute( annotationUsage, attributeName, value );
	}

	public static <A extends Annotation> void applyStringAttributeIfSpecified(
			String attributeName,
			String value,
			MutableAnnotationUsage<A> annotationUsage) {
		XmlAnnotationHelper.applyOptionalAttribute( annotationUsage, attributeName, value );
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> getOrCreateUsage(
			Class<A> annotationType,
			AnnotationTarget target,
			SourceModelBuildingContext modelBuildingContext) {
		final MutableAnnotationUsage<A> existing = (MutableAnnotationUsage<A>) target.getAnnotationUsage( annotationType );
		if ( existing != null ) {
			return existing;
		}

		final AnnotationDescriptor<A> descriptor = modelBuildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( annotationType );
		return descriptor.createUsage( target, modelBuildingContext );
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> getOrCreateUsage(
			AnnotationDescriptor<A> annotationDescriptor,
			AnnotationTarget target,
			SourceModelBuildingContext modelBuildingContext) {
		final MutableAnnotationUsage<A> existing = (MutableAnnotationUsage<A>) target.getAnnotationUsage( annotationDescriptor );
		if ( existing != null ) {
			return existing;
		}

		return annotationDescriptor.createUsage( target, modelBuildingContext );
	}
}
