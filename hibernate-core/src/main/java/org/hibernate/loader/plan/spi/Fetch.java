/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.type.Type;

/**
 * Contract for associations that are being fetched.
 * <p/>
 * NOTE : can represent components/embeddables
 *
 * @author Steve Ebersole
 */
public interface Fetch {
	/**
	 * Obtain the owner of this fetch.
	 *
	 * @return The fetch owner.
	 */
	public FetchSource getSource();

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	public PropertyPath getPropertyPath();

	/**
	 * Gets the fetch strategy for this fetch.
	 *
	 * @return the fetch strategy for this fetch.
	 */
	public FetchStrategy getFetchStrategy();

	/**
	 * Get the Hibernate Type that describes the fetched attribute
	 *
	 * @return The Type of the fetched attribute
	 */
	public Type getFetchedType();

	/**
	 * Is this fetch nullable?
	 *
	 * @return true, if this fetch is nullable; false, otherwise.
	 */
	public boolean isNullable();



	// Hoping to make these go away ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public String getAdditionalJoinConditions();

	/**
	 * Generates the SQL select fragments for this fetch.  A select fragment is the column and formula references.
	 *
	 * @return the select fragments
	 */
	public String[] toSqlSelectFragments(String alias);
}
