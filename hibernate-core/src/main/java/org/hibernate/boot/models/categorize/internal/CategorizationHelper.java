/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class CategorizationHelper {
	public static boolean isMappedSuperclass(ClassDetails classDetails) {
		return classDetails.getAnnotationUsage( JpaAnnotations.MAPPED_SUPERCLASS ) != null;
	}

	public static boolean isEntity(ClassDetails classDetails) {
		return classDetails.getAnnotationUsage( JpaAnnotations.ENTITY ) != null;
	}

	public static boolean isIdentifiable(ClassDetails classDetails) {
		return isEntity( classDetails ) || isMappedSuperclass( classDetails );
	}
}
