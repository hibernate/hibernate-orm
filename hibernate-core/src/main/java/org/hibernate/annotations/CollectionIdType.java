/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.UserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link Type} for describing the id of an id-bag mapping.
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionIdType {

	/**
	 * The custom type implementor class
	 *
	 * @see Type#value
	 */
	Class<? extends UserType<?>> value();

	/**
	 * Parameters to be injected into the custom type after
	 * it is instantiated.
	 *
	 * The type should implement {@link org.hibernate.usertype.ParameterizedType}
	 * to receive the parameters
	 *
	 * @see Type#parameters
	 */
	Parameter[] parameters() default {};
}
