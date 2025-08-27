/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.binder.internal.CollateBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a collation to use when generating DDL for
 * the column mapped by the annotated field or property.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
@Incubating
@AttributeBinderType(binder = CollateBinder.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Collate {
	/**
	 * The name of the collation.
	 */
	String value();
}
