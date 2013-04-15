/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe an index column of a List.
 *
 * @author Matthew Inger
 *
 * @deprecated Prefer the standard JPA {@link javax.persistence.OrderColumn} annotation and the Hibernate specific
 * {@link ListIndexBase} (for replacing {@link #base()}).
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface IndexColumn {
	/**
	 * The column name.
	 */
	String name();

	/**
	 * The starting index value.  Zero (0) by default, since Lists indexes start at zero (0).
	 */
	int base() default 0;

	/**
	 * Is the column nullable?
	 */
	boolean nullable() default true;

	/**
	 * An explicit column definition.
	 */
	String columnDefinition() default "";
}
