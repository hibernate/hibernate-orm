/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.exec.results.spi.InitializerSource;

/**
 * Contract for fetches.
 * <p/>
 * Note that this can represent composites/embeddables
 *
 * @author Steve Ebersole
 */
public interface Fetch extends InitializerSource {
	/**
	 * Obtain the owner of this fetch.  Ultimately used to identify
	 * the thing that "owns" this fetched navigable for the purpose of:
	 *
	 * 		* identifying the associated owner reference as we process the fetch
	 * 		* inject the fetched instance into the parent and potentially inject
	 * 			the parent reference into the fetched instance if it defines
	 * 			such injection via {@link org.hibernate.annotations.Parent}
	 */
	FetchParent getFetchParent();

	/**
	 * The Navigable being fetched
	 */
	Navigable getFetchedNavigable();

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Gets the fetch strategy for this fetch.
	 *
	 * @return the fetch strategy for this fetch.
	 */
	FetchStrategy getFetchStrategy();

	/**
	 * Is this fetch nullable?
	 *
	 * @return true, if this fetch is nullable; false, otherwise.
	 */
	boolean isNullable();
}
