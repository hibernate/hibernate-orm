/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditTable {
	/**
	 * The name of the table
	 */
	String value();

	/**
	 * The schema of the table. Defaults to the schema of the annotated entity.
	 */
	String schema() default "";

	/**
	 * The catalog of the table. Defaults to the catalog of the annotated entity.
	 */
	String catalog() default "";
}
