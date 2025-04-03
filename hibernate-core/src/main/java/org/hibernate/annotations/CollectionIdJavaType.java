/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JavaType} for describing the id of an id-bag mapping.
 *
 * @see CollectionIdJavaClass
 *
 * @since 6.0
 */
@Inherited
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionIdJavaType {
	/**
	 * The descriptor to use for the id column
	 *
	 * @see JavaType#value
	 */
	Class<? extends BasicJavaType<?>> value();
}
