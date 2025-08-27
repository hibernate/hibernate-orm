/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JdbcTypeCode} used to describe the foreign key
 * part of an {@link Any} mapping.
 *
 * @see Any
 * @see AnyKeyJdbcType
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface AnyKeyJdbcTypeCode {
	/**
	 * The code for the descriptor to use for the key column
	 *
	 * @see JdbcTypeCode#value
	 */
	int value();
}
