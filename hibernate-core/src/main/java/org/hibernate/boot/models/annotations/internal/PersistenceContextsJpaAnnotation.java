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

import jakarta.persistence.PersistenceContexts;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PersistenceContextsJpaAnnotation implements PersistenceContexts {
	public static final AnnotationDescriptor<PersistenceContexts> PERSISTENCE_CONTEXTS = null;

	private jakarta.persistence.PersistenceContext[] value;

	public PersistenceContextsJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	public PersistenceContextsJpaAnnotation(PersistenceContexts annotation, SourceModelBuildingContext modelContext) {
	}

	public PersistenceContextsJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PersistenceContexts.class;
	}

	@Override
	public jakarta.persistence.PersistenceContext[] value() {
		return value;
	}

	public void value(jakarta.persistence.PersistenceContext[] value) {
		this.value = value;
	}


}
