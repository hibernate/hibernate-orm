/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the default {@linkplain jakarta.persistence.Table#schema schema}
 * for mapping annotations in the annotated package.
 * <p>
 * When a mapping annotation occurring in the annotated package does not
 * explicitly specify its {@link jakarta.persistence.Table#schema schema},
 * the schema specified by this annotation is used.
 *
 * @since 7.4
 *
 * @author Gavin King
 */
@Target(PACKAGE)
@Retention(RUNTIME)
public @interface DefaultSchema {
	/**
	 * The default schema name.
	 */
	String value();
}
