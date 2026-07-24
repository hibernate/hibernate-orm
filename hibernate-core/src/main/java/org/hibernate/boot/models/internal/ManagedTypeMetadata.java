/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public final class ManagedTypeMetadata {
	private ManagedTypeMetadata() {
	}

	public static boolean isEntity(ClassDetails type) {
		return type.hasDirectAnnotationUsage( Entity.class );
	}

	public static boolean isMappedSuperclass(ClassDetails type) {
		return type.hasDirectAnnotationUsage( MappedSuperclass.class );
	}

	public static boolean isEmbeddable(ClassDetails type) {
		return type.hasDirectAnnotationUsage( Embeddable.class );
	}
}
