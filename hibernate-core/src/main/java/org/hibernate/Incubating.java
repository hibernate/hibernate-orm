/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks (recursively) certain of Hibernate's packages, types and methods
 * as incubating.  Incubating indicates a type or method that is still being
 * actively developed and therefore may change at a later time.  Users of these
 * types and methods are considered early adopters who help shape the final
 * definition of these types/methods.
 *
 * @author Steve Ebersole
 */
@Target({PACKAGE, TYPE, METHOD,CONSTRUCTOR})
@Retention(CLASS)
public @interface Incubating {
}
