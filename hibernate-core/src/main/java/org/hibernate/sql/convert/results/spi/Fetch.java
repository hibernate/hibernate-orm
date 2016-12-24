/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.sql.exec.results.process.spi.InitializerSource;
import org.hibernate.type.spi.Type;

/**
 * Contract for fetches.
 * <p/>
 * Note that this can represent composites/embeddables
 *
 * @author Steve Ebersole
 */
public interface Fetch extends InitializerSource {
	/**
	 * Obtain the owner of this fetch.
	 *
	 * @return The fetch owner.
	 */
	FetchParent getFetchParent();

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	PropertyPath getPropertyPath();

	/**
	 * Gets the fetch strategy for this fetch.
	 *
	 * @return the fetch strategy for this fetch.
	 */
	FetchStrategy getFetchStrategy();

	/**
	 * Get the Hibernate Type that describes the fetched attribute
	 *
	 * @return The Type of the fetched attribute
	 */
	Type getFetchedType();

	/**
	 * Is this fetch nullable?
	 *
	 * @return true, if this fetch is nullable; false, otherwise.
	 */
	boolean isNullable();
}
