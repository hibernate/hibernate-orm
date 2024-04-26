/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;

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

}
