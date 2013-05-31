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
	public static interface FetchMetadata {
		/**
		 * Is the fetch nullable?
		 *
		 * @return true, if the fetch is nullable; false, otherwise.
		 */
		public boolean isNullable();

		/**
		 * Returns the type of the fetched attribute
		 *
		 * @return the type of the fetched attribute.
		 */
		public Type getType();

		/**
		 * Generates the SQL select fragments for the specified fetch.  A select fragment is the column and formula
		 * references.
		 *
		 * @param alias The table alias to apply to the fragments (used to qualify column references)
		 *
		 * @return the select fragments
		 */
		public String[] toSqlSelectFragments(String alias);
	}

	/**
	 * Locate the metadata for the specified Fetch.  Allows easier caching of the resolved information.
	 *
	 * @param fetch The fetch for which to locate metadata
	 *
	 * @return The metadata; never {@code null}, rather an exception is thrown if the information for the fetch cannot
	 * be located.
	 */
	public FetchMetadata locateFetchMetadata(Fetch fetch);
}