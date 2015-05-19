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
 * Define a DB index.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Using {@link javax.persistence.Index} instead.
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
@Deprecated
public @interface Index {
	/**
	 * The index name.
	 */
	String name();

	/**
	 * The column(s) that are indexed.
	 */
	String[] columnNames() default {};
}
