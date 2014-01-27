/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes the column to use as the multi-tenancy discriminator value for the entity.
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target( TYPE )
@Retention( RUNTIME )
public @interface TenantColumn {
	/**
	 * Name of the column to use.
	 */
	public String name();

	/**
	 * (Optional) The name of the table that contains the column. If absent the column is assumed to be in the 
	 * primary table.
	 */
	public String table() default "";

	/**
	 * Names the Hibernate mapping type to use for mapping values to/from the specified column.  Defaults to
	 * {@code "string"} which is a {@link String}/{@link java.sql.Types#VARCHAR VARCHAR} mapping.
	 */
	public String type() default "string";

	/**
	 * (Optional) The column length. (Applies only if a string-valued column is used.)
	 */
	int length() default 255;

	/**
	 * (Optional) The precision for a decimal (exact numeric) column. (Applies only if a decimal column is used.)
	 * Value must be set by developer if used when generating the DDL for the column.
	 */
	int precision() default 0;

	/**
	 * (Optional) The scale for a decimal (exact numeric) column. (Applies only if a decimal column is used.)
	 */
	int scale() default 0;
}
