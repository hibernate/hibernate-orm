/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.JoinColumn;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a generated nested annotation
 * type with the same name as a foreign key
 * column.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface JoinColumnMapping {
	JoinColumn value();
}
