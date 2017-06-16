/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.exec.results.spi.InitializerSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

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
	 * The Navigable being fetched.
	 */
	NavigableReference getFetchedNavigableReference();

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
