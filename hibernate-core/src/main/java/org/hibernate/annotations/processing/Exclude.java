/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Indicates that a package or top-level type should be ignored
 * by the Hibernate annotation processor.
 *
 * @author Gavin King
 * @since 6.5
 */
@Target({PACKAGE, TYPE})
@Retention(CLASS)
@Incubating
public @interface Exclude {
}
