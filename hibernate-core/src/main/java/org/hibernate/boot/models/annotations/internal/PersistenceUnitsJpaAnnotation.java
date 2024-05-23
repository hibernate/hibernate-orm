/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AttributeDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import java.lang.annotation.Annotation;

import jakarta.persistence.PersistenceUnits;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PersistenceUnitsJpaAnnotation implements PersistenceUnits {
	public static final AnnotationDescriptor<PersistenceUnits> PERSISTENCE_UNITS = null;

	private jakarta.persistence.PersistenceUnit[] value;

	public PersistenceUnitsJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	public PersistenceUnitsJpaAnnotation(PersistenceUnits annotation, SourceModelBuildingContext modelContext) {
	}

	public PersistenceUnitsJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PersistenceUnits.class;
	}

	@Override
	public jakarta.persistence.PersistenceUnit[] value() {
		return value;
	}

	public void value(jakarta.persistence.PersistenceUnit[] value) {
		this.value = value;
	}


}
