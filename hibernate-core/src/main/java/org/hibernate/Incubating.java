/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the annotated Java element as <em>incubating</em>, potentially
 * recursively. An incubating API is one which is still being actively
 * developed and therefore may change at a later time; a "tech preview".
 * The user of such an API is considered an early adopter who helps shape
 * the final definition of the API.
 *
 * @implNote Defined with {@code RUNTIME} retention so tooling can see it
 *
 * @author Steve Ebersole
 */
@Target({PACKAGE, TYPE, ANNOTATION_TYPE, METHOD, FIELD, CONSTRUCTOR})
@Retention(RUNTIME)
@Documented
public @interface Incubating {
}
