/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the maximum length of a SQL array type mapped by
 * the annotated attribute.
 * <p>
 * For example:
 * <pre>
 * {@code @Array(length=100)} // the maximum length of the SQL array
 * {@code @Column(length=64)} // the maximum length of the strings in the array
 * String[] strings;
 * </pre>
 */
@Incubating
@Target({FIELD, METHOD})
@Retention( RUNTIME )
public @interface Array {
	/**
	 * The maximum length of the array.
	 */
	int length();
}
