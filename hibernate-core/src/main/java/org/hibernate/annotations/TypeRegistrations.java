/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Grouping of {@link TypeRegistration}
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
public @interface TypeRegistrations {
	TypeRegistration[] value();
}
