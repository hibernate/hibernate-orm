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
package org.hibernate.loader.plan2.spi;

import org.hibernate.loader.PropertyPath;

/**
 * Contract for a FetchSource (aka, the thing that owns the fetched attribute).
 *
 *
 * @author Steve Ebersole
 */
public interface FetchSource {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	public static final Fetch[] NO_FETCHES = new Fetch[0];

	/**
	 * Get the property path to this fetch owner
	 *
	 * @return The property path
	 */
	public PropertyPath getPropertyPath();

	public String getQuerySpaceUid();

	/**
	 * Retrieve the fetches owned by this return.
	 *
	 * @return The owned fetches.
	 */
	public Fetch[] getFetches();

	/**
	 * Resolve the "current" {@link EntityReference}, or null if none.
	 *
	 * If this object is an {@link EntityReference}, then this object is returned.
	 *
	 * If this object is a {@link CompositeFetch}, then the nearest {@link EntityReference}
	 * will be resolved from its source, if possible.
	 *
	 * If no EntityReference can be resolved, null is return.
	 *
	 *  @return the "current" EntityReference or null if none.
	 *  otherwise, if this object is also a {@link Fetch}, then
	 * .
	 * @see org.hibernate.loader.plan2.spi.Fetch#getSource().
	 * 	 */
	public EntityReference resolveEntityReference();

	// Stuff I can hopefully remove ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The idea of addFetch() below has moved to {@link org.hibernate.loader.plan2.build.spi.ExpandingFetchSource}.
	 * <p/>
	 * Most of the others are already part of Fetch
	 */


//
//
//
//	/**
//	 * Returns the type of the specified fetch.
//	 *
//	 * @param fetch - the owned fetch.
//	 *
//	 * @return the type of the specified fetch.
//	 */
//	public Type getType(Fetch fetch);
//
//	/**
//	 * Is the specified fetch nullable?
//	 *
//	 * @param fetch - the owned fetch.
//	 *
//	 * @return true, if the fetch is nullable; false, otherwise.
//	 */
//	public boolean isNullable(Fetch fetch);
//
//	/**
//	 * Generates the SQL select fragments for the specified fetch.  A select fragment is the column and formula
//	 * references.
//	 *
//	 * @param fetch - the owned fetch.
//	 * @param alias The table alias to apply to the fragments (used to qualify column references)
//	 *
//	 * @return the select fragments
//	 */
//	public String[] toSqlSelectFragments(Fetch fetch, String alias);
//
//	/**
//	 * Contract to add fetches to this owner.  Care should be taken in calling this method; it is intended
//	 * for Hibernate usage
//	 *
//	 * @param fetch The fetch to add
//	 */
//	public void addFetch(Fetch fetch);
//
//
//	public SqlSelectFragmentResolver toSqlSelectFragmentResolver();
//
//
//

}
