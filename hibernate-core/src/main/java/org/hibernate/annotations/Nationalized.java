/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated character data should be stored with
 * nationalization support. The effect of this annotation depends on
 * {@link Dialect#getNationalizationSupport() the SQL dialect}.
 * <ul>
 *     <li>Some databases support storing nationalized data using their
 *         "normal" character data types
 *         ({@code CHAR, VARCHAR, LONGVARCHAR, CLOB}).
 *         For these dialects, this annotation is effectively ignored.
 *         See {@link org.hibernate.dialect.NationalizationSupport#IMPLICIT}.
 *     <li>Other databases support storing nationalized data only via the
 *         specialized, standard SQL variants
 *         ({@code NCHAR, NVARCHAR, LONGNVARCHAR, NCLOB)}.
 *         For these dialects, this annotation will adjust the JDBC type
 *         code to use the specialized variant.
 *         See {@link org.hibernate.dialect.NationalizationSupport#EXPLICIT}.
 * </ul>
 *
 * @author Steve Ebersole
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface Nationalized {
}
