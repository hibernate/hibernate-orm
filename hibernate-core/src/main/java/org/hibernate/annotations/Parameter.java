/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generic parameter (a key/value pair) used to parametrize other annotations.
 *
 * @author Emmanuel Bernard
 *
 * @see Type#parameters
 * @see CollectionType#parameters
 * @see CollectionIdType#parameters
 * @see MapKeyType#parameters
 * @see GenericGenerator#parameters
 */
@Target({})
@Retention(RUNTIME)
public @interface Parameter {
	/**
	 * The parameter name.
	 */
	String name();

	/**
	 * The parameter value.
	 */
	String value();
}
