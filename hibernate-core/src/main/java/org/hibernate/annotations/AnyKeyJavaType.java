/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JavaType} used to describe the foreign-key part of an ANY mapping.
 *
 * @see Any
 * @see AnyKeyJavaClass
 *
 * @since 6.0
 */
@Target({METHOD, FIELD,ANNOTATION_TYPE})
@Retention( RUNTIME )
public @interface AnyKeyJavaType {
	/**
	 * The type descriptor to use
	 *
	 * @see JavaType#value
	 */
	Class<? extends BasicJavaType<?>> value();
}
