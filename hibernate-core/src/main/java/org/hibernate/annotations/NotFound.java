/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Action to do when an element is not found on a association.
 *
 * @author Emmanuel Bernard
 */
@Target( { METHOD, FIELD })
@Retention(RUNTIME)
public @interface NotFound {
	/**
	 * The action to perform when an associated entity is not found.  By default an exception is thrown.
	 */
	NotFoundAction action() default NotFoundAction.EXCEPTION;
}
