/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.jdbc.JdbcType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link org.hibernate.annotations.JdbcType} used to
 * describe the foreign key part of an {@link Any} mapping.
 *
 * @see Any
 * @see AnyKeyJdbcTypeCode
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface AnyKeyJdbcType {
	/**
	 * The descriptor to use for the key column
	 *
	 * @see org.hibernate.annotations.JdbcType#value
	 */
	Class<? extends JdbcType> value();
}
