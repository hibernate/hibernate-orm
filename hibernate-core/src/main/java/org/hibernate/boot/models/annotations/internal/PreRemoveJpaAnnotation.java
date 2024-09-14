/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.PreRemove;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PreRemoveJpaAnnotation implements PreRemove {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PreRemoveJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PreRemoveJpaAnnotation(PreRemove annotation, SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PreRemoveJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PreRemove.class;
	}
}
