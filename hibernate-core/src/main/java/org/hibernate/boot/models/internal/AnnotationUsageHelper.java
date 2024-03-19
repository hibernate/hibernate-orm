package org.hibernate.boot.models.internal;

import java.lang.annotation.Annotation;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;

public class AnnotationUsageHelper {

	public static <A extends Annotation> void applyAttributeIfSpecified(
			String attributeName,
			Object value,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( value != null ) {
			annotationUsage.setAttributeValue( attributeName, value );
		}
	}

	public static <A extends Annotation> void applyStringAttributeIfSpecified(
			String attributeName,
			String value,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( StringHelper.isNotEmpty( value ) ) {
			annotationUsage.setAttributeValue( attributeName, value );
		}
	}
}
