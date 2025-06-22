/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link org.hibernate.annotations.JdbcType} for describing
 * the column mapping for the index of a {@code List} or array.
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface ListIndexJdbcType {
	/**
	 * The descriptor to use for the list-index column
	 *
	 * @see org.hibernate.annotations.JdbcType#value
	 */
	Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value();
}
