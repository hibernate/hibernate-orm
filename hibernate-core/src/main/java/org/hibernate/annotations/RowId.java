/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that a {@code rowid}-like column or pseudo-column should be
 * used as the row locator in CRUD operations for an entity,
 * instead of the primary key of the table.
 * <p>
 * If the {@linkplain org.hibernate.dialect.Dialect SQL dialect} does
 * not support some sort of {@code rowid}-like column or pseudo-column,
 * then this annotation is ignored, and the primary key is used as the
 * row locator.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.dialect.Dialect#rowId
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RowId {
	/**
	 * Specifies the name of the {@code rowid}-like column for databases
	 * where the column is declared explicitly in DDL.
	 * <p>
	 * It is <em>not</em> necessary to specify the name for databases where
	 * the {@code rowid}-like value is an implicitly-existing pseudo-column,
	 * and on those databases, this annotation member is ignored.
	 *
	 * @apiNote Previously, this annotation member was required. But the
	 *          name of the column it is now usually determined by calling
	 *          {@link org.hibernate.dialect.Dialect#rowId}, and so this
	 *          member is now usually ignored. The exception is for certain
	 *          flavors of DB2.
	 */
	String value() default "";
}
