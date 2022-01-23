/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

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
 * Marks certain of packages, types, etc. as incubating, potentially
 * recursively.  Incubating indicates something that is still being
 * actively developed and therefore may change at a later time; a
 * "tech preview".
 * <p/>
 * Users of these types and methods are considered early adopters who
 * help shape the final definition of these types/methods, along with
 * the needs of consumers.
 *
 * @implNote Defined with RUNTIME retention so tooling can see it
 *
 * @author Steve Ebersole
 */
@Target({PACKAGE, TYPE, ANNOTATION_TYPE, METHOD, FIELD, CONSTRUCTOR})
@Retention(RUNTIME)
public @interface Incubating {
}
