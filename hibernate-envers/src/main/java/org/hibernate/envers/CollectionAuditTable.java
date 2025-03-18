/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

/**
 * Allows for the customization of an Envers audit collection table.
 *
 * @author Chris Cranford
 */
@Incubating
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CollectionAuditTable {
	/**
	 * The name of the table
	 */
	String name();

	/**
	 * The schema of the table.  Defaults to the schema of the mapping.
	 */
	String schema() default "";

	/**
	 * The catalog of the table.  Defaults to the catalog of the mapping.
	 */
	String catalog() default "";
}
