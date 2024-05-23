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

import jakarta.persistence.PersistenceContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PersistenceContextJpaAnnotation implements PersistenceContext {
	public static final AnnotationDescriptor<PersistenceContext> PERSISTENCE_CONTEXT = null;

	private String name;
	private String unitName;
	private jakarta.persistence.PersistenceContextType type;
	private jakarta.persistence.SynchronizationType synchronization;
	private jakarta.persistence.PersistenceProperty[] properties;

	public PersistenceContextJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.unitName = "";
		this.type = jakarta.persistence.PersistenceContextType.TRANSACTION;
		this.synchronization = jakarta.persistence.SynchronizationType.SYNCHRONIZED;
		this.properties = new jakarta.persistence.PersistenceProperty[0];
	}

	public PersistenceContextJpaAnnotation(PersistenceContext annotation, SourceModelBuildingContext modelContext) {
	}

	public PersistenceContextJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PersistenceContext.class;
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


	@Override
	public jakarta.persistence.PersistenceContextType type() {
		return type;
	}

	public void type(jakarta.persistence.PersistenceContextType value) {
		this.type = value;
	}


	@Override
	public jakarta.persistence.SynchronizationType synchronization() {
		return synchronization;
	}

	public void synchronization(jakarta.persistence.SynchronizationType value) {
		this.synchronization = value;
	}


	@Override
	public jakarta.persistence.PersistenceProperty[] properties() {
		return properties;
	}

	public void properties(jakarta.persistence.PersistenceProperty[] value) {
		this.properties = value;
	}


}
