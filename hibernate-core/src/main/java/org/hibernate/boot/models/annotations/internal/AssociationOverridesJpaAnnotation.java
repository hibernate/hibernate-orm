/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AssociationOverrides;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AssociationOverridesJpaAnnotation implements AssociationOverrides {
	private jakarta.persistence.AssociationOverride[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AssociationOverridesJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AssociationOverridesJpaAnnotation(AssociationOverrides annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, JpaAnnotations.ASSOCIATION_OVERRIDES, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AssociationOverridesJpaAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.AssociationOverride[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AssociationOverrides.class;
	}

	@Override
	public jakarta.persistence.AssociationOverride[] value() {
		return value;
	}

	public void value(jakarta.persistence.AssociationOverride[] value) {
		this.value = value;
	}


}
