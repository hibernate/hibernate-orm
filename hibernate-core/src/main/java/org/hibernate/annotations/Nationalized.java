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
 * Used to indicate that the annotated character data should be stored with
 * nationalization support.
 *
 * The annotation's affect depends on {@link Dialect#getNationalizationSupport()}
 *
 * Some databases support storing nationalized data in their "normal" character data types
 * (CHAR, VARCHAR, LONGVARCHAR, CLOB).  In such cases this annotation is effectively ignored.
 * See {@link org.hibernate.dialect.NationalizationSupport#IMPLICIT}
 *
 * Some databases support storing nationalized data only through the specialized, standard SQL
 * variants (NCHAR, NVARCHAR, LONGNVARCHAR, NCLOB).  In such cases this annotation will adjust
 * the JDBC type code to the specialized variant. See
 * {@link org.hibernate.dialect.NationalizationSupport#EXPLICIT}
 *
 * @author Steve Ebersole
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface Nationalized {
}
