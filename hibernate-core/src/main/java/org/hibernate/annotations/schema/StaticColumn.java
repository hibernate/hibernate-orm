/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.JDBCType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a generated nested annotation
 * type with the same name as a column.
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface StaticColumn {
	String name();
	JDBCType type();
}
