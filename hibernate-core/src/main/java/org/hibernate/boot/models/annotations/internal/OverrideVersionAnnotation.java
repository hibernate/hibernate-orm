/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OverrideVersionAnnotation implements DialectOverride.Version {
	private int major;
	private int minor;

	public OverrideVersionAnnotation(SourceModelBuildingContext modelContext) {
		this.minor = 0;
	}

	public OverrideVersionAnnotation(DialectOverride.Version annotation, SourceModelBuildingContext modelContext) {
		major( annotation.major() );
		minor( annotation.minor() );
	}

	public OverrideVersionAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DialectOverride.Version.class;
	}

	@Override
	public int major() {
		return major;
	}

	public void major(int value) {
		this.major = value;
	}

	@Override
	public int minor() {
		return minor;
	}

	public void minor(int value) {
		this.minor = value;
	}
}
