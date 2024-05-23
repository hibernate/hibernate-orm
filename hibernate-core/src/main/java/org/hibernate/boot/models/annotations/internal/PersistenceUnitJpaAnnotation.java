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

import jakarta.persistence.PersistenceUnit;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PersistenceUnitJpaAnnotation implements PersistenceUnit {
	public static final AnnotationDescriptor<PersistenceUnit> PERSISTENCE_UNIT = null;

	private String name;
	private String unitName;

	public PersistenceUnitJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.unitName = "";
	}

	public PersistenceUnitJpaAnnotation(PersistenceUnit annotation, SourceModelBuildingContext modelContext) {
	}

	public PersistenceUnitJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PersistenceUnit.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String unitName() {
		return unitName;
	}

	public void unitName(String value) {
		this.unitName = value;
	}


}
