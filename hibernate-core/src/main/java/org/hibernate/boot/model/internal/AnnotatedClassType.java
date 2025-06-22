/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


import org.hibernate.annotations.Imported;

/**
 * Type of annotation of a class will give its type
 *
 * @author Emmanuel Bernard
 */
public enum AnnotatedClassType {
	/**
	 * has no relevant top level annotation
	 */
	NONE,
	/**
	 * has an {@link Imported} annotation
	 */
	IMPORTED,
	/**
	 * has an {@link jakarta.persistence.Entity} annotation
	 */
	ENTITY,
	/**
	 * has an {@link jakarta.persistence.Embeddable} annotation
	 */
	EMBEDDABLE,
	/**
	 * has {@link jakarta.persistence.MappedSuperclass} annotation
	 */
	MAPPED_SUPERCLASS
}
