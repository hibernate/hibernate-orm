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
package org.hibernate.loader.plan.spi;

import org.hibernate.type.Type;

/**
 * This interface provides a delegate for a fetch owner to obtain details about an owned fetch.
 *
 * @author Gail Badner
 */
public interface FetchOwnerDelegate {

	/**
	 * Is the specified fetch nullable?
	 *
	 * @param fetch - the owned fetch.
	 *
	 * @return true, if the fetch is nullable; false, otherwise.
	 */
	public boolean isNullable(Fetch fetch);

	/**
	 * Returns the type of the specified fetch.
	 *
	 * @param fetch - the owned fetch.
	 *
	 * @return the type of the specified fetch.
	 */
	public Type getType(Fetch fetch);

	/**
	 * Returns the column names used for loading the specified fetch.
	 *
	 * @param fetch - the owned fetch.
	 *
	 * @return the column names used for loading the specified fetch.
	 */
	public String[] getColumnNames(Fetch fetch);
}