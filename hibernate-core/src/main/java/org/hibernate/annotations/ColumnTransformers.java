/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Plural annotation for @ColumnTransformer.
 * Useful when more than one column is using this behavior.
 *
 * @author Emmanuel Bernard
 */
@Target({FIELD,METHOD})
@Retention(RUNTIME)
public @interface ColumnTransformers {
	/**
	 * The aggregated transformers.
	 */
	ColumnTransformer[] value();
}
