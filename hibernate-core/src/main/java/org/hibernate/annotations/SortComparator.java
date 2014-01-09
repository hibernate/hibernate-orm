/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Specifies in-memory Set/Map sorting using a specified {@link Comparator} for sorting.
 *
 * NOTE : Sorting is different than ordering (see {@link OrderBy}) which is applied during the SQL SELECT.
 *
 * For sorting based on natural sort order, use {@link SortNatural} instead.  It is illegal to combine
 * {@link SortComparator} and {@link SortNatural}.
 *
 * @see OrderBy
 * @see SortComparator
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SortComparator {
	/**
	 * Specifies the comparator class to use.
	 */
	Class<? extends Comparator<?>> value();
}
