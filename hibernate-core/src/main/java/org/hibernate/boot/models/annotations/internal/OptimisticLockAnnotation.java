/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OptimisticLockAnnotation implements OptimisticLock {
	private boolean excluded;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OptimisticLockAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OptimisticLockAnnotation(OptimisticLock annotation, SourceModelBuildingContext modelContext) {
		this.excluded = annotation.excluded();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OptimisticLockAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.excluded = extractJandexValue(
				annotation,
				HibernateAnnotations.OPTIMISTIC_LOCK,
				"excluded",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OptimisticLock.class;
	}

	@Override
	public boolean excluded() {
		return excluded;
	}

	public void excluded(boolean value) {
		this.excluded = value;
	}


}
