/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that the annotated element is planned for removal as part of a
 * deprecation process.
 *
 * @apiNote Intended for use at development time for developers to better
 * understand the lifecycle of the annotated element. Also, useful for
 * deprecating a whole package, since the Java compiler does not accept
 * the {@link Deprecated @Deprecated} annotation on packages.
 *
 * @see Deprecated#forRemoval()
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD, TYPE, PACKAGE, CONSTRUCTOR, TYPE_PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface Remove {
}
