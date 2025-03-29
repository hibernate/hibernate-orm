/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.persistence.JoinColumn;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface AuditJoinTable {
	/**
	 * Name of the join table. Defaults to a concatenation of the names of the primary table of the entity
	 * owning the association and of the primary table of the entity referenced by the association.
	 */
	String name() default "";

	/**
	 * The schema of the join table. Defaults to the schema of the entity owning the association.
	 */
	String schema() default "";

	/**
	 * The catalog of the join table. Defaults to the catalog of the entity owning the association.
	 */
	String catalog() default "";

	/**
	 * The foreign key columns of the join table which reference the primary table of the entity that does not
	 * own the association (i.e. the inverse side of the association).
	 */
	JoinColumn[] inverseJoinColumns() default {};
}
