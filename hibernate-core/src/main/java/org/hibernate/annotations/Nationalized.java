/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated character data should be stored with
 * nationalization support. The effect of this annotation depends on
 * {@linkplain Dialect#getNationalizationSupport() the SQL dialect}.
 * <ul>
 *     <li>Some databases support storing nationalized data using their
 *         "normal" character data types ({@code CHAR, VARCHAR, CLOB}).
 *         For these dialects, this annotation is effectively ignored.
 *         <p>
 *         See {@link org.hibernate.dialect.NationalizationSupport#IMPLICIT}.
 *     <li>Other databases support storing nationalized data only via the
 *         specialized, standard SQL variants ({@code NCHAR, NVARCHAR, NCLOB)}.
 *         For these dialects, this annotation will adjust the JDBC type
 *         code to use the specialized variant.
 *         <p>
 *         See {@link org.hibernate.dialect.NationalizationSupport#EXPLICIT}.
 * </ul>
 *
 * @see org.hibernate.dialect.NationalizationSupport
 * @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
 *
 * @author Steve Ebersole
 */
@Target( { METHOD, FIELD, ANNOTATION_TYPE } )
@Retention( RUNTIME )
public @interface Nationalized {
}
