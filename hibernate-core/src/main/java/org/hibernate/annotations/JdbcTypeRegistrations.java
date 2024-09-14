/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Grouping of {@link JdbcTypeRegistration}
 *
 * See notes on {@link JdbcTypeRegistration} about using on packages
 * versus use on classes
 *
 * @since 6.0
 */
@Target({PACKAGE, TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcTypeRegistrations {
	JdbcTypeRegistration[] value();
}
