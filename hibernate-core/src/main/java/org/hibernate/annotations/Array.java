/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/// Specifies the maximum length of a SQL array type mapped by
/// the annotated attribute.
///
/// For example:
///
/// ```java
/// @Array(length=100) // the maximum length of the SQL array
/// @Column(length=64) // the maximum length of the strings in the array
/// String[] strings;
/// ```
@Incubating
@Target({FIELD, METHOD})
@Retention( RUNTIME )
public @interface Array {
	/// The maximum length of the array.
	int length();
}
