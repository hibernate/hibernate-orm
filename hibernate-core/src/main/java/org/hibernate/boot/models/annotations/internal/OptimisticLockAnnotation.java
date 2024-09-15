/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.OptimisticLock;
import org.hibernate.models.spi.SourceModelBuildingContext;

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
	public OptimisticLockAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.excluded = (boolean) attributeValues.get( "excluded" );
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
