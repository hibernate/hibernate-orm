/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that the annotated element is planned for removal as part
 * of a deprecation lifecycle.
 *
 * @apiNote Intended for use at development-time for developers to better understand
 * the lifecycle of the annotated element.
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD, TYPE, PACKAGE, CONSTRUCTOR, TYPE_PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Remove {
}
