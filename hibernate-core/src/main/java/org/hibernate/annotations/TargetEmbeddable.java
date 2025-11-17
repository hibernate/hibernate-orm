/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the target type for a {@link jakarta.persistence.Embedded} entity property.
 * <p>
 * It's intended to be used exclusively in conjunction with @Embedded.
 */
@java.lang.annotation.Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
public @interface TargetEmbeddable {
	/**
	 * The target type for a {@link jakarta.persistence.Embedded} entity property
	 */
	Class<?> value();
}
