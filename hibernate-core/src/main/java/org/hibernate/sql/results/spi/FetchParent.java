/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;

/**
 * Contract for things that can be the parent of a fetch
 *
 * @author Steve Ebersole
 */
public interface FetchParent {
	NavigableContainer getNavigableContainer();

	/**
	 * Get the property path to this parent
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Retrieve the fetches owned by this fetch source.
	 *
	 * @return The owned fetches.
	 */
	List<Fetch> getFetches();

	/**
	 * todo (6.0) : this would be needed in order to apply fetching on top of an existing graph
	 *
	 * 		- pass in the `Navigable` or `Fetchable` instead?
	 *
	 *
	 * todo (6.0) : to properly support bytecode-based laziness, any state-array-contributor (including basic types) would need to be "fetchable"
	 */
	default Fetch findFetch(String fetchableName) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
