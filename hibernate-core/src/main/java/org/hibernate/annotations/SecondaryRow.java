/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies how the row of a {@link jakarta.persistence.SecondaryTable} should be managed.
 *
 * @see jakarta.persistence.SecondaryTable
 *
 * @since 6.2
 * @author Gavin King
 */
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(SecondaryRows.class)
public @interface SecondaryRow {
	/**
	 * The name of the secondary table, as specified by
	 * {@link jakarta.persistence.SecondaryTable#name()}.
	 */
	String table() default "";

	/**
	 * If disabled, Hibernate will never insert or update the columns of the secondary table.
	 * <p>
	 * This setting is useful if data in the secondary table belongs to some other entity,
	 * or if it is maintained externally to Hibernate.
	 */
	boolean owned() default true;

	/**
	 * Unless disabled, specifies that no row should be inserted in the secondary table if
	 * all the columns of the secondary table would be null. Furthermore, an outer join will
	 * always be used to read the row. Thus, by default, Hibernate avoids creating a row
	 * containing only null column values.
	 */
	boolean optional() default true;
}
