/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Repeatable container for {@link CollectionTypeRegistration}
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionTypeRegistrations {
	/**
	 * The individual CollectionTypeRegistration
	 */
	CollectionTypeRegistration[] value();
}
