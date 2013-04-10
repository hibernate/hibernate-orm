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

/**
 * Defines the start index value for a list index as stored on the database.  This base is subtracted from the
 * incoming database value on reads to determine the List position; it is added to the List position index when
 * writing to the database.
 *
 * By default list indexes are stored starting at zero.
 *
 * Generally used in conjunction with {@link javax.persistence.OrderColumn}.
 *
 * @see javax.persistence.OrderColumn
 *
 * @author Steve Ebersole
 */
public @interface ListIndexBase {
	/**
	 * The list index base.  Default is 0.
	 */
	int value() default 0;
}
