/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.internal.Target;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TargetXmlAnnotation implements Target {
	private String value;

	public TargetXmlAnnotation(SourceModelBuildingContext modelContext) {
	}

	public TargetXmlAnnotation(Target annotation, SourceModelBuildingContext modelContext) {
		throw new UnsupportedOperationException( "Should only ever be sourced from XML" );
	}

	public TargetXmlAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		throw new UnsupportedOperationException( "Should only ever be sourced from XML" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Target.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


}
